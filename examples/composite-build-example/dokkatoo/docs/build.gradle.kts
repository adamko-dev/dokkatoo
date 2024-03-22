plugins {
  id("dokka-convention")
}

dependencies {
  dokkatoo("foo.example:module-kakapo")
  dokkatoo("foo.example:module-kea")
}

dokkatoo {
  moduleName = "Dokkatoo Composite Builds Example"
}
