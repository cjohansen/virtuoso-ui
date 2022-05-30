(ns virtuoso.login-page.page-test
  (:require [clojure.test :refer [deftest is]]
            [ui.event-bus :as bus]
            [virtuoso.login-page.page :as sut]
            [virtuoso.test-helper :as helper]))

(def location {:location/page-id :virtuoso.pages/login-pages})

(def scenarios
  [[::initial-state sut/page
    {}
    location]

   [::typed-email sut/page
    {:transient {location {:email "christian@"}}}
    location]

   [::valid-email sut/page
    {:transient {location {:email "christian@cjohansen.no"}}}
    location]

   [::pending-otp sut/page
    {:transient {location {:email "christian@cjohansen.no"}}
     :picard/executions {:login/request-otp {:status :in-progress}}}
    location]

   [::waiting-for-otp-input sut/page
    {:transient {location {:email "christian@cjohansen.no"}}
     :picard/executions {:login/request-otp
                         {:status :success
                          :command [:login/request-otp "christian@cjohansen.no"]}}}
    location]

   [::pending-login sut/page
    {:transient {location {:email "christian@cjohansen.no"}}
     :picard/executions {:login/request-otp
                         {:status :success
                          :command [:login/request-otp "christian@cjohansen.no"]}
                         :login/authenticate {:status :in-progress}}}
    location]])

(deftest regression-tests
  (helper/run-regression-tests scenarios))

(deftest complete-authentication-test
  (is (= (->> {:data {:token "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"}}
              (sut/complete-authentication {}))
         [[:actions/assoc-in
           [:token] "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"
           [:token-info] {:email "christian@cjohansen.no"}]])))

(deftest complete-login-test
  (is (= (->> {:data {:token "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"}}
              (sut/complete-login {}))
         [[:actions/assoc-in
           [:token] "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"
           [:token-info] {:email "christian@cjohansen.no"}]
          [:actions/go-to-location {:location/page-id :virtuoso.pages/home-page}]])))

(deftest register-actions--completes-login-when-picard-command-completes
  (is (= (let [app (helper/create-test-app)]
           (sut/register-actions app)
           (bus/publish
            (:event-bus app)
            [:ui.picard/complete-execution :login/authenticate]
            {:data {:token "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"}})
           (drop 1 @(:event-log app)))
         [[:actions/assoc-in
           [:token] "ejY.eyJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.sig"
           [:token-info] {:email "christian@cjohansen.no"}]
          [:actions/go-to-location #:location{:page-id :virtuoso.pages/home-page}]])))

(deftest register-actions--completes-authentication-when-token-refreshes
  (is (= (let [app (helper/create-test-app)]
           (sut/register-actions app)
           (bus/publish
            (:event-bus app)
            [:ui.picard/complete-execution :login/refresh-token]
            {:data {:token "ejY.eyJlbWFpbCI6ImJvYkBjbG93bi5jb20ifQ.sig"}})
           (drop 1 @(:event-log app)))
         [[:actions/assoc-in
           [:token] "ejY.eyJlbWFpbCI6ImJvYkBjbG93bi5jb20ifQ.sig"
           [:token-info] {:email "bob@clown.com"}]])))

(comment

  (helper/try-scenario (last scenarios))

  (helper/find-failing-scenarios scenarios)
  (helper/diagnose-first-failure scenarios)
  (helper/update-first-failing scenarios)
  (helper/update-all-failing scenarios)
  (helper/inspect-failure-diffs scenarios)

)
