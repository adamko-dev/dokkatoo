import {sortBy} from "@site/src/utils/jsUtils";
import React from "react";
import JarFileIcon from "@site/static/img/icons/jar-file.svg";
import ThreeDotsIcon from "@site/static/img/icons/three-dots.svg";
import ThemedImage from "@theme/ThemedImage";

export type TagType =
    | "favourite"

    | "android"
    | "java"
    | "kotlinJvm"
    | "kotlinMultiplatform"

    | "customPlugin"
    | "versioning"

    | "design" // add 'design' if there's _some_ customization
    | "example"
    | "large"
    | "javadocJar"

    | "gfm"
    | "html"
    | "jekyll"
    | "javadoc"


// please sort sites alphabetically
const Users: User[] = [
  {
    title: "androidx-ktx-extras",
    description: "Extra KTX modules for AndroidX.",
    preview: <ThemedImage
        alt={`androidx website screenshot`}
        sources={{
          light: require("./showcase/androidx-ktx-extras-light.png").default,
          dark: require("./showcase/androidx-ktx-extras-dark.png").default,
        }}
    />,
    website: "https://edricchan03.github.io/androidx-ktx-extras/",
    source: "https://github.com/EdricChan03/androidx-ktx-extras/blob/browser-ktx%400.1.0/build.gradle.kts",
    tags: ["html", "android", "javadocJar"],
  },
  {
    title: "Apollo Kotlin",
    description: "A strongly-typed, caching GraphQL client for Java and Kotlin multiplatform.",
    preview: <ThemedImage
        alt={`Apollo website screenshot`}
        sources={{
          light: require("./showcase/apollo-graphql-light.png").default,
          dark: require("./showcase/apollo-graphql-dark.png").default,
        }}
    />,
    website: "https://www.apollographql.com/docs/kotlin/kdoc/",
    source: "https://github.com/apollographql/apollo-kotlin/blob/v4.0.0-beta.5/build-logic/src/main/kotlin/Publishing.kt",
    tags: ["favourite", "kotlinMultiplatform", "html", "design", "large", "versioning"],
  },
  {
    title: "Dokkatoo",
    description: "Generates documentation for Kotlin Gradle projects.",
    preview: <ThemedImage
        alt={`Dokkatoo website screenshot`}
        sources={{
          light: require("./showcase/dokkatoo-light.png").default,
          dark: require("./showcase/dokkatoo-dark.png").default,
        }}
    />,
    website: "https://adamko-dev.github.io/dokkatoo/",
    source: "https://github.com/adamko-dev/dokkatoo/blob/v2.2.0/modules/dokkatoo-plugin/build.gradle.kts#L217-L235",
    tags: ["html", "kotlinJvm"],
  },
  {
    title: "Gradle",
    description: "Adaptable, fast automation for all.",
    preview: <ThemedImage
        alt={`Gradle website screenshot`}
        sources={{
          light: require("./showcase/gradle-light.png").default,
          dark: require("./showcase/gradle-dark.png").default,
        }}
    />,
    website: "https://docs.gradle.org/current/kotlin-dsl/",
    source: "https://github.com/gradle/gradle/tree/v8.6.0/build-logic/documentation",
    tags: ["favourite", "java", "kotlinJvm", "large", "html"],
  },
  {
    title: "Gradle central-release-publishing",
    description: "An opinionated Gradle plugin to manage publishing to Maven Central.",
    preview: <JarFileIcon className="java-filetype"/>,
    website: null, // not published
    source: "https://github.com/evant/gradle-central-release-publishing/blob/v0.1.0/src/main/kotlin/me/tatarka/gradle/publishing/CentralReleasePublishingPlugin.kt#L107-L138",
    tags: ["kotlinJvm", "html", "javadocJar"],
  },
  {
    title: "GW2ChatLinks",
    description: "A Kotlin Multiplatform library for parsing and generating Guild Wars 2 chat links.",
    preview: <ThemedImage
        alt={`$GW2ChatLinks website screenshot`}
        sources={{
          light: require("./showcase/GW2ChatLinks-light.png").default,
          dark: require("./showcase/GW2ChatLinks-dark.png").default,
        }}
    />,
    website: "https://gw2toolbelt.github.io/GW2ChatLinks/latest/api/",
    source: "https://github.com/GW2ToolBelt/GW2ChatLinks/blob/4dcce081987f731aa816d0562e486a705680bc07/build.gradle.kts#L156-L183",
    tags: ["kotlinMultiplatform", "html", "javadocJar"],
  },
  {
    title: "GW2ToolBelt api-generator",
    description: "A library for creating programs that interface with data exposed by the official Guild Wars 2 API.",
    preview: <JarFileIcon className="java-filetype"/>,
    website: null, // it appears a new version hasn't been published since Dokkatoo was added
    source: "https://github.com/GW2ToolBelt/api-generator/blob/2059cd9883a9eb9347e66679c42e471bf48e28e4/build.gradle.kts#L59-L83",
    tags: ["html", "javadocJar"],
  },
  {
    title: "KobanKat",
    description: "RevenueCat SDK for Kotlin Multiplatform.",
    preview: <ThemedImage
        alt={`KobanKat website screenshot`}
        sources={{
          light: require("./showcase/KobanKat-light.png").default,
          dark: require("./showcase/KobanKat-dark.png").default,
        }}
    />,
    website: "https://kobankat.shortway.io/",
    source: "https://github.com/diffplug/selfie/tree/jvm/2.0.1/jvm/gradle/dokka",
    tags: ["kotlinMultiplatform", "html"],
  },
  {
    title: "Kolibrium",
    description: "Kotlin library for Selenium tests.",
    preview: <JarFileIcon className="java-filetype"/>,
    website: null,
    source: "https://github.com/attila-fazekas/kolibrium/blob/33e724bcb8f34d6a3654fee92a83c5b045454f21/buildSrc/src/main/kotlin/kolibrium.library-conventions.gradle.kts#L23",
    tags: ["kotlinJvm", "html", "javadocJar"],
  },
  {
    title: "Kotka Streams",
    description: "Kotka Streams - the Kotlin DSL for Kafka Streams.",
    preview: <ThemedImage
        alt={`Kotka website screenshot`}
        sources={{
          light: require("./showcase/kotka-streams-light.png").default,
          dark: require("./showcase/kotka-streams-dark.png").default,
        }}
    />,
    website: "https://adamko-dev.github.io/kotka-streams/",
    source: "https://github.com/adamko-dev/kotka-streams/blob/v23.03.13/buildSrc/src/main/kotlin/buildsrc/convention/dokkatoo.gradle.kts",
    tags: ["kotlinJvm", "html"],
  },
  {
    title: "Kotlin/JS Resources Plugin",
    description: "Expose resources transitively to downstream projects.",
    preview: <ThemedImage
        alt={`Kotlin/JS Resources Plugin website screenshot`}
        sources={{
          light: require("./showcase/Kotlin-JS-Resources-Plugin-light.png").default,
          dark: require("./showcase/Kotlin-JS-Resources-Plugin-dark.png").default,
        }}
    />,
    website: "https://opensavvy.gitlab.io/automation/kotlin-js-resources/api-docs/",
    source: "https://gitlab.com/opensavvy/automation/kotlin-js-resources",
    tags: ["kotlinMultiplatform", "html"],
  },
  {
    title: "KS3",
    description: "KotlinX Serialization Standard Serializers (KS3).",
    preview: <ThemedImage
        alt={`KS3 website screenshot`}
        sources={{
          light: require("./showcase/ks3-light.png").default,
          dark: require("./showcase/ks3-dark.png").default,
        }}
    />,
    website: "https://www.ks3.io/",
    source: "https://github.com/Kantis/ks3/blob/v0.6.0/build.gradle.kts",
    tags: ["kotlinMultiplatform", "html", "javadoc", "javadocJar"],
  },
  {
    title: "ModuleCheck",
    description: "Fast dependency graph validation for Gradle.",
    preview: <ThemedImage
        alt={`$ModuleCheck website screenshot`}
        sources={{
          light: require("./showcase/ModuleCheck-light.png").default,
          dark: require("./showcase/ModuleCheck-dark.png").default,
        }}
    />,
    website: "https://rickbusarow.github.io/ModuleCheck/api/index.html",
    source: "https://github.com/rickbusarow/ModuleCheck/blob/ff891da3b08cf1bd375e6617c36baa6e98e2e0fe/build-logic/conventions/src/main/kotlin/modulecheck/builds/DokkatooConventionPlugin.kt#L53",
    tags: ["kotlinJvm", "html", "large"],
  },
  {
    title: "OSS Review Toolkit (ORT)",
    description: "A suite of tools to automate software compliance checks.",
    preview: <ThemedImage
        alt={`OSS website screenshot`}
        sources={{
          light: require("./showcase/oss-review-toolkit-light.png").default,
          dark: require("./showcase/oss-review-toolkit-dark.png").default,
        }}
    />,
    // docs are published per-module (not aggregated)
    website: "https://javadoc.io/doc/org.ossreviewtoolkit/cli/latest/",
    source: "https://github.com/oss-review-toolkit/ort/blob/18.0.0/buildSrc/src/main/kotlin/ort-kotlin-conventions.gradle.kts#L186-L193",
    tags: ["kotlinJvm", "javadoc", "javadocJar"],
  },
  {
    title: "Pedestal",
    description: "Progress and failure tracking, architectures for aggressive caching.",
    preview: <ThemedImage
        alt={`Pedestal website screenshot`}
        sources={{
          light: require("./showcase/Pedestal-light.png").default,
          dark: require("./showcase/Pedestal-dark.png").default,
        }}
    />,
    website: "https://opensavvy.gitlab.io/prepared/api-docs/suite/index.html",
    source: "https://gitlab.com/opensavvy/pedestal",
    tags: ["kotlinMultiplatform", "html"],
  },
  {
    title: "Prepared",
    description: "Magicless testing framework for Kotlin Multiplatform.",
    preview: <ThemedImage
        alt={`Prepared website screenshot`}
        sources={{
          light: require("./showcase/Prepared-light.png").default,
          dark: require("./showcase/Prepared-dark.png").default,
        }}
    />,
    website: "https://opensavvy.gitlab.io/prepared/api-docs/suite/index.html",
    source: "https://gitlab.com/opensavvy/prepared",
    tags: ["favourite", "kotlinMultiplatform", "html"],
  },
  {
    title: "Selfie",
    description: "Snapshot testing for Java, Kotlin, and the JVM.",
    preview: <ThemedImage
        alt={`Selfie website screenshot`}
        sources={{
          light: require("./showcase/selfie-light.png").default,
          dark: require("./showcase/selfie-dark.png").default,
        }}
    />,
    website: "https://kdoc.selfie.dev/",
    source: "https://github.com/diffplug/selfie/tree/jvm/2.0.1/jvm/gradle/dokka",
    tags: ["favourite", "kotlinJvm", "html", "design"],
  },

  // looks like a convention plugin, not an actual project
  // {
  //   title: "DiffPlug Blowdryer scripts",
  //   description: "DiffPlug's scripts for use with blowdryer.",
  //   preview: require("./showcase/dokkatoo-dark.png.default,
  //   website: "https://github.com/diffplug/blowdryer-diffplug",
  //   source:
  // "https://github.com/diffplug/blowdryer-diffplug/blob/7.2.0/src/main/resources/base/maven.gradle#L66-L71",
  // tags: ["kotlinMultiplatform", "javadoc", "javadocJar"], },


  // https://github.com/fluxo-kt/fluxo-kmp-conf - uses Dokka, mentions Dokkatoo, but hasn't migrated
  // https://github.com/rickbusarow/mahout
  // https://github.com/rickbusarow/kase

  //region examples
  {
    title: "Custom Format Example",
    description: "This example demonstrates how to override `.css` styles and add custom images as assets, allowing you to change the logo used in the header.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/custom-format-example/dokkatoo",
    tags: ["example", "design", "kotlinJvm", "html"],
  },
  {
    title: "Gradle Example",
    description: "This example demonstrates how to apply Dokka in a simple single-project Gradle build, as well as how to configure it.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/gradle-example/dokkatoo",
    tags: ["example", "kotlinJvm", "html", "jekyll", "gfm", "javadoc"],
  },
  {
    title: "Kotlin-as-Java Example",
    description: "This example demonstrates how you can apply a Dokka plugin in a simple Gradle project.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/kotlin-as-java-example/dokkatoo",
    tags: ["example", "kotlinJvm", "customPlugin", "html", "jekyll", "gfm", "javadoc"],
  },
  {
    title: "Kotlin Multiplatform Example",
    description: "This example demonstrates Dokka's configuration and output for a simple Kotlin Multiplatform project.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/multiplatform-example/dokkatoo",
    tags: ["example", "kotlinMultiplatform", "html", "jekyll", "gfm", "javadoc"],
  },
  // {
  //   title: "Library Publishing Example",
  //   description: "This example demonstrates how you can integrate Dokka into the publishing
  // process of your library, adding documentation generated by Dokka as artifacts.", preview:
  // <ThreeDotsIcon/>, website: null, source:
  // "https://github.com/adamko-dev/dokkatoo/tree/main/examples/library-publishing-example/dokkatoo",
  // tags: ["example", "kotlinJvm", "javadocJar"], },
  {
    title: "Multimodule Example",
    description: "This example demonstrates Dokka's configuration and output for a simple Kotlin Multiplatform project.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/multiplatform-example/dokkatoo",
    tags: ["example", "kotlinJvm", "html"],
  },
  {
    title: "Versioning Example",
    description: "This example demonstrates configuration of Dokka's Versioning plugin, which allows readers to navigate through different versions of your API reference documentation.",
    preview: <ThreeDotsIcon/>,
    website: null,
    source: "https://github.com/adamko-dev/dokkatoo/tree/main/examples/versioning-multimodule-example/dokkatoo",
    tags: ["example", "kotlinJvm", "versioning", "html"],
  },
  //endregion
];

export type User = {
  title: string;
  description: string;
  preview: React.JSX.Element,
  website: string | null;
  source: string | null;
  tags: TagType[];
};

export type Tag = {
  label: string;
  description: string;
  color: string;
};

export const Tags: { [type in TagType]: Tag } = {
  favourite: {
    label: "Favourite",
    description: "Our favourite Dokkatoo sites.",
    color: "#E9669EFF",
  },

  android: {
    label: "Android",
    description: "Sites for Android projects",
    color: "#00ff00",
  },
  java: {
    label: "Java",
    description: "Sites for Java projects",
    color: "#808000",
  },
  kotlinJvm: {
    label: "Kotlin JVM",
    description: "Sites for Kotlin JVM projects",
    color: "#d2b48c",
  },
  kotlinMultiplatform: {
    label: "Kotlin Multiplatform",
    description: "Sites for Kotlin Multiplatform projects",
    color: "#ff0000",
  },

  example: {
    label: "Examples",
    description: "Simple demonstration projects, for learning the basics.",
    color: "#1e90ff",
  },
  design: {
    label: "Design",
    description: "Beautiful Dokkatoo sites, polished and standing out from the initial template!",
    color: "#2f4f4f",
  },
  versioning: {
    label: "Versioning",
    description: "Dokkatoo sites using the Versioning plugin, to manage multiple versions.",
    color: "#2e8b57",
  },
  customPlugin: {
    label: "Custom Plugins",
    description: "This site uses a custom Dokka Plugin to modify the output.",
    color: "#191970",
  },
  large: {
    label: "Large",
    description: "Very large Dokkatoo sites, including many more classes or modules than the average!",
    color: "#8b0000",
  },
  javadocJar: {
    label: "Javadoc JAR",
    description: "Dokkatoo is used to create a Javadoc JAR.",
    color: "#00ffff",
  },

  html: {
    label: "HTML",
    description: "Generates a site in an HTML format.",
    color: "#ff8c00",
  },
  javadoc: {
    label: "Javadoc",
    description: "Generates a site in a Javadoc format.",
    color: "#0000cd",
  },
  jekyll: {
    label: "Jekyll",
    description: "Generates a site in a Jekyll Flavoured Markdown format.",
    color: "#98fb98",
  },
  gfm: {
    label: "Markdown",
    description: "Generates a site in a GitHub Flavoured Markdown (GFM) format.",
    color: "#ba55d3",
  },
  /*
  additional distinct colours:
    #1e90ff
    #fa8072
    #ffff54
    #dda0dd
    #ff1493

    #87cefa
   */
};

export const TagList = Object.keys(Tags) as TagType[];

function sortUsers() {
  let result = Users;
  // Sort by site name
  result = sortBy(result, (user) => user.title.toLowerCase());
  // Sort by favourite tag, favourites first
  result = sortBy(result, (user) => !user.tags.includes("favourite"));
  return result;
}

export const sortedUsers = sortUsers();
