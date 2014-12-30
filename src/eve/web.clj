(ns eve.web
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :as ring]))

(defroutes routes
  (GET "/" [] "<h2>Hello World</h2>"))

(def server
  (ring/run-jetty #'routes {:port 8080 :join? false}))
