plugins {
  id("dokka-convention")
}

dependencies {
  dokkatoo("foo.example:module-kakapo")
  dokkatoo("foo.example:module-kea")
}

dokkatoo {
  moduleName.set("Dokkatoo Composite Builds Example")
}
