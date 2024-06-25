plugins {
  id("kotlin-jvm-convention")
  id("dokka-convention")
}

group = "foo.example"
version = "1.2.3"

dokkatoo {
  moduleName.set("Kakapo Module")
  modulePath.set("kakakpo")
}
