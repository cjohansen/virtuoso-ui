ifndef GIT_SHA
GIT_SHA = $$(git rev-parse --short=10 HEAD)
endif

rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))

target/public: $(call rwildcard,src/,*.cljs) $(call rwildcard,prod/,*.cljs) $(call rwildcard,resources/,*)  $(call rwildcard,build/,*)
	GIT_SHA=$(GIT_SHA) clojure -A:build -X virtuoso.build/build --path target

clean:
	rm -fr target

.phony: clean
