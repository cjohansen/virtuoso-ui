# Virtuoso UI

This repo contains the user interface for
[https://virtuosoapp.net](https://virtuosoapp.net), a webapp that helps
musicians practice their instruments more effectively, using techniques backed
by cognitive science.

## Structure and interpretation of this repo

This repo contains a homemade "framework" in `src/ui`. Why homemade? Because
homemade frameworks are the best: they're lean because they only cater to your
very specific needs, they can be easily changed and expanded as needed, and they
don't change when your needs don't. Additionally, the Virtuoso app-specific
logic and components can be found in `src/virtuoso`.

## Up and running

To develop this UI, run a figwheel REPL. My preferred way is to fire up Emacs
and do `cider-jack-in-cljs`. If you're not an Emacs user, you can instead try:

```clj
clojure -A:dev -m figwheel.main -b dev
```

## Production builds

To build the frontend for production, run:

```sh
make target/public
```

This process will output a fully standalone UI in `target/public`. It needs to
be served from the Virtuoso server to communicate successfully with the backend.
