(ns objective8.integration.front-end.comments
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.core :as core]))

(def USER_ID 1)
(def COMMENT_ID 123)
(def OBJECTIVE_ID 234)


(facts "comments"
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve comment against an objective"
              (against-background
                  (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success})
              (against-background
                  (http-api/create-comment {:comment "The comment"
                                            :objective-id OBJECTIVE_ID
                                            :created-by-id USER_ID}) => {:status ::http-api/success
                                                                         :result  {:_id 12
                                                                                   :objective-id OBJECTIVE_ID
                                                                                   :created-by-id USER_ID
                                                                                   :comment "The comment"}})
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}})
               (let [user-session (helpers/test-context)
                     params {:comment "The comment"
                             :objective-id (str OBJECTIVE_ID)}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                                          (p/request "http://localhost:8080/comments"
                                                     :request-method :post
                                                     :params params))]
                 peridot-response => (helpers/flash-message-contains "Your comment has been added!")
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID))))))
