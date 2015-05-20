(ns objective8.integration.front-end.comments
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.core :as core]))

(def USER_ID 1)
(def COMMENT_ID 123)
(def OBJECTIVE_ID 234)
(def GLOBAL_ID 223)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))

(def user-session (helpers/test-context))

(facts "about posting comments"
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve comment against an objective"
              (against-background
               (http-api/post-comment {:comment "The comment"
                                       :comment-on-uri OBJECTIVE_URI
                                       :created-by-id USER_ID}) => {:status ::http-api/success
                                                                    :result  {:_id 12
                                                                              :created-by-id USER_ID
                                                                              :comment-on-uri OBJECTIVE_URI
                                                                              :comment "The comment"}})
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}})
               (let [params {:comment "The comment"
                             :refer (str "/objectives/" OBJECTIVE_ID)
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                                              (p/request (utils/path-for :fe/post-comment)
                                                         :request-method :post
                                                         :params params))]
                 (:flash response) => (contains {:type :flash-message :message :comment-view/created-message})
                 (:headers response) => (helpers/location-contains (str "/objectives/" OBJECTIVE_ID))
                 (:status response) => 302))))

(facts "about viewing comments"
       (binding [config/enable-csrf false]
         (fact "gets comments starting from the requested offset"
               (against-background
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result {:uri OBJECTIVE_URI
                                                                   :meta {:comments-count 5}}})
               (-> user-session
                   (p/request (str (utils/path-for :fe/get-comments-for-objective
                                                   :id OBJECTIVE_ID)
                                   "?offset=50"))
                   :response
                   :status)
               => 200
               (provided
                (http-api/get-comments OBJECTIVE_URI {:offset 50}) => {:status ::http-api/success
                                                                       :result []}))

         (fact "anyone can view comments for an objective"
               (against-background
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result {:uri OBJECTIVE_URI
                                                                   :meta {:comments-count 5}}}
                (http-api/get-comments anything anything) => {:status ::http-api/success
                                                              :result [{:_id 1
                                                                        :_created_at "2015-02-12T16:46:18.838Z"
                                                                        :objective-id OBJECTIVE_ID
                                                                        :created-by-id USER_ID
                                                                        :comment "Comment 1"
                                                                        :uri "/comments/1"
                                                                        :votes {:up 123456789 :down 987654321}}]})
               (-> (p/request user-session (utils/path-for :fe/get-comments-for-objective
                                                           :id OBJECTIVE_ID))
                   :response
                   :body) => (contains "Comment 1"))

         (fact "user can see the range of comments currently being viewed"
               (against-background
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result {:uri OBJECTIVE_URI
                                                                   :meta {:comments-count 75}}}
                (http-api/get-comments OBJECTIVE_URI anything) => {:status ::http-api/success
                                                                   :result []})
               (-> user-session
                   (p/request (utils/path-for :fe/get-comments-for-objective
                                              :id OBJECTIVE_ID))
                   :response
                   :body) => (contains #"1.+-.+50.+of.+75")

               (-> user-session
                   (p/request (str (utils/path-for :fe/get-comments-for-objective
                                                   :id OBJECTIVE_ID)
                                   "?offset=50"))
                   :response
                   :body) => (contains #"51.+-.+75.+of.+75"))))
