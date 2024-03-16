package buildsrc.screenshotter

import java.io.Serializable
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class Website @Inject constructor(
  @Internal
  protected val named: String,
) : Named, Serializable {
  @get:Input
  abstract val uri: Property<URI>
  @get:Input
  @get:Optional
  abstract val enabled: Property<Boolean>

  @Input
  override fun getName(): String = named
}
