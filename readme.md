Scala.jsFiddle
==============

Source code for [www.scala-js-fiddle.com](http://www.scala-js-fiddle.com). To develop, install [sbt-revolver](https://github.com/spray/sbt-revolver) and run:

```
sbt "~; macros/package; server/re-start; client/optimizeJS"
```

This will start the server at `localhost:8080`, which you can go to and immediately start the live editing process.
