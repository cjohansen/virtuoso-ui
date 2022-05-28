(ns ui.router-test
  (:require [ui.router :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest parse-query-params-test
  (is (= (sut/parse-query-params "haha=lol&yep&something=one two three&other=one%20two")
         {:haha "lol", :yep true, :something "one two three", :other "one two"}))

  (is (= (sut/parse-query-params "items=32")
         {:items 32}))

  (is (= (sut/parse-query-params "sum=32.2")
         {:sum 32.2}))

  (is (= (sut/parse-query-params "included=true")
         {:included true}))

  (is (= (sut/parse-query-params "included=false")
         {:included false}))

  (is (nil? (sut/parse-query-params ""))))

(deftest encode-query-params-test
  (is (= (sut/parse-query-params "haha=lol&yep&something=one%20two%20three&other=one%20two")
         (-> "haha=lol&yep&something=one%20two%20three&other=one%20two"
             sut/parse-query-params
             sut/encode-query-params
             sut/parse-query-params))))

(deftest parse-url-to-route-test
  (is (= (sut/parse-url-to-route "/tasks/317/files?tab=pdf")
         {:location/route-data ["tasks" "317" "files"]
          :location/query-params {:tab "pdf"}})))

(deftest matches-route?-test
  (is (true?
       (sut/matches-route?
        {:location/route ["tasks" :id :section]}
        (sut/parse-url-to-route "/tasks/317/files?tab=pdf"))))

  (is (not
       (sut/matches-route?
        {:location/route ["tasks" :id :section "bleh"]}
        (sut/parse-url-to-route "/tasks/317/files?tab=pdf"))))

  (is (not
       (sut/matches-route?
        {:location/route ["tasks" :id "bleh"]}
        (sut/parse-url-to-route "/tasks/317/files"))))

  (is (true?
       (sut/matches-route?
        {:location/route []}
        (sut/parse-url-to-route "/"))))

  (is (true?
       (sut/matches-route?
        {:location/route []}
        (sut/parse-url-to-route "/?stuff")))))

(deftest resolve-route-test
  (is (= (sut/resolve-route
          {:page/task-detail-type {:location/route ["tasks" :id :type]
                                   :location/page-id :page/task-detail-type}
           :page/homepage {:location/route []
                           :location/page-id :page/homepage}}
          "/tasks/317/files?tab=pdf")
         {:location/page-id :page/task-detail-type
          :location/params {:id "317", :type "files"}
          :location/query-params {:tab "pdf"}}))

  (is (= (sut/resolve-route
          {:page/task-detail-type {:location/route ["tasks" :id :type]
                                   :location/page-id :page/task-detail-type}
           :page/homepage {:location/route []
                           :location/page-id :page/homepage}}
          "/?tab=pdf")
         {:location/page-id :page/homepage
          :location/query-params {:tab "pdf"}})))

(deftest url-to-test
  (is (= (let [pages {:page/task-detail-type {:location/route ["tasks" :id :type]
                                              :location/page-id :page/task-detail-type}}]
           (->> "/tasks/317/files?tab=pdf"
                (sut/resolve-route pages)
                (sut/url-to pages)))
         "/tasks/317/files?tab=pdf"))

  (is (= (let [pages {:page/home {:location/route []
                                  :location/page-id :page/home}}]
           (sut/url-to pages {:location/page-id :page/home}))
         "/")))
