package fiddle
import scala.scalajs.js
import js.Dynamic.{global, literal => lit}
import org.scalajs.dom
import collection.mutable
import scalatags.Tags2

object Page{
  import scalatags.all._
  def body = Seq(
    pre(id:="editor")(starting),
    pre(id:="logspam")(
      """
        |- Enter your code on the left pane =)
        |- Command/Ctrl-Enter to compile and execute your program
        |- Draw pictures on the right pane and see println()s in the browser console
      |""".stripMargin
    ),
    div(id:="sandbox")(
      canvas(id:="canvas")
    )
  )
  val starting =
    """
      |import org.scalajs.dom
      |
      |case class Pt(x: Double, y: Double)
      |object ScalaJSExample{
      |  println("Hello!!")
      |  val sandbox = dom.document
      |    .getElementById("canvas")
      |    .asInstanceOf[dom.HTMLCanvasElement]
      |
      |  val renderer = sandbox.getContext("2d")
      |    .asInstanceOf[dom.CanvasRenderingContext2D]
      |
      |  val corners = Seq(
      |    Pt(sandbox.width/2, 0),
      |    Pt(0, sandbox.height),
      |    Pt(sandbox.width, sandbox.height)
      |  )
      |  var p = corners(0)
      |  val (w, h) = (sandbox.height.toDouble, sandbox.height.toDouble)
      |  def main(args: Array[String]): Unit = {
      |    dom.setInterval(() => for(_ <- 0 until 10){
      |      val c = corners(util.Random.nextInt(3))
      |      p = Pt((p.x + c.x) / 2, (p.y + c.y) / 2)
      |      val m = (p.y / h)
      |      val r = 255 - (p.x / w * m * 255).toInt
      |      val g = 255 - ((w-p.x) / w * m * 255).toInt
      |      val b = 255 - ((h - p.y) / h * 255).toInt
      |      renderer.fillStyle = s"rgb($r, $g, $b)"
      |      renderer.fillRect(p.x, p.y, 1, 1)
      |    }, 10)
      |  }
      |}
    """.stripMargin

}

object Client{

  var requestInFlight = false
  def sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  def canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]

  def clear() = {
    println(sandbox.clientHeight + " " + sandbox.clientWidth)
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    sandbox.innerHTML = sandbox.innerHTML
  }
  def log(s: Any): Unit = {
    logspam.textContent += s + "\n"
    logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight
  }
  def main(args: Array[String]): Unit = {
    dom.document.body.innerHTML = Page.body.mkString
    clear()

    val editor = global.ace.edit("editor")
    editor.setTheme("ace/theme/twilight")
    editor.getSession().setMode("ace/mode/scala")
    editor.renderer.setShowGutter(false)

    val callback = { () =>
      val code = editor.getSession().getValue().asInstanceOf[String]

      if (!requestInFlight){
        val req = new dom.XMLHttpRequest()
        requestInFlight = true
        log("Compiling...")
        req.onload = { (e: dom.Event) =>
          requestInFlight = false
          try{
            val result = js.JSON.parse(req.responseText)
            dom.console.log(result)
            logspam.textContent += result.logspam
            if(result.success.asInstanceOf[js.Boolean]){
              clear()
              js.eval(""+result.code)
              log("Success")
            }else{
              log("Failure")
            }

          }catch{case e =>
            log(req.responseText)
            log("Failure")
          }
        }
        req.open("POST", "/compile")
        req.send(code)
      }
    }

    editor.commands.addCommand(lit(
      name = "saveFile",
      bindKey = lit(
        win = "Ctrl-Enter",
        mac = "Command-Enter",
        sender = "editor|cli"
      ),
      exec = callback: js.Function0[_]
    ))
    editor.getSession().setTabSize(2)
    callback()
  }
}