(ns lib.log)

(defn log [x]
  (js/console.log (clj->js x)))
