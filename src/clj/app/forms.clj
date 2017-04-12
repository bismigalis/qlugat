(ns app.forms)

(def login-form
  [{:id :email
    :label "Email"
    :type :string
    :validator (fn [username]
                 (cond
                  (not (seq username))
                  "Name can't be empty"
                  (try (Long/parseLong username)
                       (catch Exception _ nil))
                  "Name can't be a number"))}
   {:id :password
    :label "Password"
    :type :password
    :validator (fn [password]
                 (when (not (seq password))
                   "Password can't be empty"))}])
