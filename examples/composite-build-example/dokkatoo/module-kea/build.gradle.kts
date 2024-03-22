plugins {
  id("kotlin-jvm-convention")
  id("dokka-convention")
}

group = "foo.example"
version = "4.5.6"

dokkatoo {
  moduleName = "Kea Module"
  modulePath = "kea"
}
