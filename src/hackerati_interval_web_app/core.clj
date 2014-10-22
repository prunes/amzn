; Run test server with `lein ring server-headless`
; Run -main with `lein run 3000`
; If lein run is crashing, you are probably forgetting the port number.
(ns hackerati-interval-web-app.core
  (:require  [compojure.core :refer :all]
             [compojure.handler :as handler]
             [compojure.route :as route]
             [hackerati-interval-web-app.views :as views]
             [hackerati-interval-web-app.schema :as db]
             [ring.adapter.jetty :as jetty]
             [ring.middleware.defaults :refer :all]
             [ring.middleware.session.cookie])
  (:use [ring.middleware.json :only [wrap-json-response]])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)) 
  (:gen-class))

(defroutes main-routes
  (GET "/" request (views/index request))
  (POST "/delete-link" request (views/delete-link! request))
  (GET "/link/:productid" request (views/link-view request))
  (GET "/login" {session :session}  (views/not-logged-in session))
  (POST "/login" request (views/login request))
  (POST "/newlink" request (views/new-link request))
  (POST "/register" request (views/attempt-register request))
  (GET "/test" request (str "{\"foo\": \"bar\"}"))
  (route/resources "/bootstrap")
  (route/not-found "<h1>Page not found</h1>"))

;; TODO Add back anti-forgery when site goes live.
;; TODO replace cookie key
(def app
  (wrap-json-response
   (wrap-defaults
    (handler/site main-routes)
     (-> (assoc-in site-defaults [:security :anti-forgery] false)  
         (assoc-in [:store] (ring.middleware.session.cookie/cookie-store {:key "a 16-byte secret"}))))
   :pretty))

(defn- scheduled-task [f]
  (-> (ScheduledThreadPoolExecutor. 10)
      (.scheduleAtFixedRate f 0 24 TimeUnit/HOURS)))

;; Scheduled task must go first, otherwise won't get executed.
(defn -main
  ([] (-main 3000))
  ([port]
     (if-let [port (try 
                     (Integer/parseInt port) 
                     (catch Exception e))] 
       (do
         (System/setProperty "java.awt.headless" "true")
         (scheduled-task db/refresh-prices)
         (jetty/run-jetty app 
                          {:port port :auto-reload? true}))
       (println "Useage: lein run port"))))

