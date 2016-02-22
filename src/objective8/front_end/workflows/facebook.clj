(ns objective8.front-end.workflows.facebook
  (:require [ring.util.response :as response]
            [objective8.utils :as utils]
            [objective8.front-end.api.http :as http]
            [cheshire.core :as json]
            [bidi.ring :refer [make-handler]]))

(def redirect-uri (str utils/host-url "/facebook-callback"))

(defn facebook-sign-in [{:keys [facebook-config] :as request}]
  (let [client-id (:client-id facebook-config)]
    (response/redirect (str "https://www.facebook.com/dialog/oauth?client_id=" client-id
                            "&redirect_uri=" redirect-uri))))

(defn response->json [response]
  (json/parse-string (:body response) true))

(defn get-access-token [{:keys [params facebook-config]}]
  (let [code (:code params)
        client-id (:client-id facebook-config)
        client-secret (:client-secret facebook-config)]
    (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     client-id
                                                                                           :redirect_uri  redirect-uri
                                                                                           :client_secret client-secret
                                                                                           :code          code}})))

(defn get-token-info [{:keys [facebook-config]} access-token]
  (let [client-id (:client-id facebook-config)
        client-secret (:client-secret facebook-config)]
    (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                               :access_token (str client-id "|" client-secret)}})))

(defn facebook-callback [{:keys [session] :as request}]
  (let [access-token-response (get-access-token request)
        access-token (:access_token (response->json access-token-response))
        token-info-response (get-token-info request access-token)
        fb-user-id (str "facebook-" (get-in (response->json token-info-response) [:data :user-id]))]
    (into (response/redirect (str utils/host-url "/sign-up"))
          {:session (assoc session :auth-provider-user-id fb-user-id)})))

(def facebook-routes
  ["/" {"facebook-sign-in"  :sign-in
        "facebook-callback" :callback}])

(defn wrap-handler [handler facebook-config]
  (fn [request] (handler (assoc request :facebook-config facebook-config))))

(defn facebook-handlers [facebook-config]
  {:sign-in       (wrap-handler facebook-sign-in facebook-config)
   :callback      (wrap-handler facebook-callback facebook-config)})

(defn facebook-workflow [facebook-config]
  (make-handler facebook-routes (facebook-handlers facebook-config)))