(ns tnoda-clojure.example.heroku-http-kit-websocket
  (:require [org.httpkit.server
             :refer [with-channel websocket? send!
                     on-receive on-close run-server]]
            [clojure.core.async
             :refer [chan sliding-buffer go go-loop timeout <! put!]]))

(defn- mod-add
  [x y]
  (rem (+ x y) 1000000007))


(defn- input->long
  [s]
  (let [c->l #(-> % long (- 48) (max 0))
        f    #(-> % (* 10) (mod-add %2))]
    (->> s (map c->l) (reduce f))))

(defn app
  [req] ; ring-request
  (with-channel req ws
    (let [c0 (doto (chan (sliding-buffer 1))
               (put! 0))
          c1 (chan)
          c2 (chan)]
      ;; go-loop #1, time loop
      (go-loop []
        (let [x (<! c0)]
          (put! c1 x)
          (put! c2 x))
        (<! (timeout 2000))
        (recur))
      ;; go-loop #2, sender
      (go-loop []
        (let [x (<! c1)]
          (send! ws (str x)))
        (recur))
      ;; go-loop #3, generator
      (go-loop []
        (->> c2 <!
             (mod-add (rand-int 10000))
             (put! c0))
        (recur))
      ;; receiver
      (on-receive ws #(->> % input->long (put! c0))))))

(defn -main
  [& args]
  (run-server app {:port (Long/parseLong (System/getenv "PORT"))}))
