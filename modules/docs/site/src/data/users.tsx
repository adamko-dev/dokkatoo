import {sortBy} from "@site/src/utils/jsUtils";
import React from "react";
import JarFileIcon from "@site/static/img/icons/jar-file.svg";

export type TagType =
    | "favourite"
    | "design" // add 'design' if there's _some_ customisation
    | "versioning"
    | "customPlugin"
    | "large"
    | "android"
    | "java"
    | "kotlinJvm"
    | "kotlinMultiplatform"
    | "html"
    | "javadoc"
    | "jekyll"
    | "gfm"
    | "javadocJar"

// please sort sites alphabetically
const Users: User[] = [
  {
    title: "androidx-ktx-extras",
    description: "Extra KTX modules for AndroidX.",
    preview: require("./showcase/androidx-ktx-extras-dark.png").default,
    website: "https://edricchan03.github.io/androidx-ktx-extras/",
    source: "https://github.com/EdricChan03/androidx-ktx-extras/blob/browser-ktx%400.1.0/build.gradle.kts",
    tags: ["html", "android", "javadocJar"],
  },
  {
    title: "Apollo Kotlin",
    description: "A strongly-typed, caching GraphQL client for Java and Kotlin multiplatform.",
    preview: require("./showcase/apollo-graphql-dark.png").default,
    website: "https://www.apollographql.com/docs/kotlin/kdoc/",
    source: "https://github.com/apollographql/apollo-kotlin/blob/v4.0.0-beta.5/build-logic/src/main/kotlin/Publishing.kt",
    tags: ["favourite", "kotlinMultiplatform", "html", "design", "large", "versioning"],
  },
  {
    title: "Dokkatoo",
    description: "Generates documentation for Kotlin Gradle projects.",
    preview: require("./showcase/dokkatoo-dark.png").default,
    website: "https://adamko-dev.github.io/dokkatoo/",
    source: "https://github.com/adamko-dev/dokkatoo/blob/v2.2.0/modules/dokkatoo-plugin/build.gradle.kts#L217-L235",
    tags: ["favourite", "html"],
  },
  {
    title: "Gradle",
    description: "Adaptable, fast automation for all.",
    preview: require("./showcase/gradle-dark.png").default,
    website: "https://docs.gradle.org/current/kotlin-dsl/",
    source: "https://github.com/gradle/gradle/tree/v8.6.0/build-logic/documentation",
    tags: ["java", "kotlinJvm", "large", "html"],
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
    title: "GW2ToolBelt api-generator",
    description: "A library for creating programs that interface with data exposed by the official Guild Wars 2 API.",
    preview: <JarFileIcon className="java-filetype"/>,
    website: null, // it appears a new version hasn't been published since Dokkatoo was added
    source: "https://github.com/GW2ToolBelt/api-generator/blob/2059cd9883a9eb9347e66679c42e471bf48e28e4/build.gradle.kts#L59-L83",
    tags: ["html", "javadocJar"],
  },
  {
    title: "Kotka Streams",
    description: "Kotka Streams - the Kotlin DSL for Kafka Streams.",
    preview: require("./showcase/kotka-streams-dark.png").default,
    website: "https://adamko-dev.github.io/kotka-streams/",
    source: "https://github.com/adamko-dev/kotka-streams/blob/v23.03.13/buildSrc/src/main/kotlin/buildsrc/convention/dokkatoo.gradle.kts",
    tags: ["kotlinJvm", "html"],
  },
  {
    title: "KS3",
    description: "KotlinX Serialization Standard Serializers (KS3).",
    preview: require("./showcase/ks3-dark.png").default,
    website: "https://www.ks3.io/",
    source: "https://github.com/Kantis/ks3/blob/v0.6.0/build.gradle.kts",
    tags: ["kotlinMultiplatform", "html", "javadoc", "javadocJar"],
  },
  {
    title: "OSS Review Toolkit (ORT)",
    description: "A suite of tools to automate software compliance checks.",
    preview: require("./showcase/oss-review-toolkit-dark.png").default,
    // docs are published per-module (not aggregated)
    website: "https://javadoc.io/doc/org.ossreviewtoolkit/cli/latest/",
    source: "https://github.com/oss-review-toolkit/ort/blob/18.0.0/buildSrc/src/main/kotlin/ort-kotlin-conventions.gradle.kts#L186-L193",
    tags: ["kotlinJvm", "javadoc", "javadocJar"],
  },
  {
    title: "Selfie",
    description: "Snapshot testing for Java, Kotlin, and the JVM.",
    preview: require("./showcase/selfie-dark.png").default,
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


  // TODO
  //      https://github.com/GW2ToolBelt/GW2ChatLinks
  //      https://github.com/fluxo-kt/fluxo-kmp-conf
  //      https://github.com/rickbusarow/mahout
  //      https://github.com/rickbusarow/matrix-gradle-plugin
  //      https://github.com/gagarski/vertigram
  //      https://github.com/rickbusarow/ModuleCheck
  //      https://github.com/attila-fazekas/kolibrium
  //      https://github.com/CLOVIS-AI/Prepared
  //      https://github.com/rickbusarow/kase
  //      https://github.com/CLOVIS-AI/Pedestal
  //      https://github.com/rickbusarow/Doks
  //      https://github.com/JayShortway/kobankat
  //      https://github.com/rickbusarow/statik

];

export type User = {
  title: string;
  description: string;
  preview: string | React.JSX.Element;
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
    description: "My favourite Dokkatoo sites.",
    color: "#E9669EFF",
  },

  design: {
    label: "Design",
    description: "Beautiful Docusaurus sites, polished and standing out from the initial template!",
    color: "#2f4f4f",
  },

  versioning: {
    label: "Versioning",
    description: "Dokkatoo sites using the versioning feature of the docs plugin to manage multiple versions.",
    color: "#2e8b57",
  },

  customPlugin: {
    label: "Custom Plugins",
    description: "Dokkatoo sites using the versioning feature of the docs plugin to manage multiple versions.",
    color: "#191970",
  },

  large: {
    label: "Large",
    description: "Very large Dokkatoo sites, including many more pages than the average!",
    color: "#8b0000",
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

  html: {
    label: "HTML",
    description: "HTML output format",
    color: "#ff8c00",
  },

  javadoc: {
    label: "Javadoc",
    description: "HTML output format",
    color: "#0000cd",
  },

  jekyll: {
    label: "Jekyll",
    description: "HTML output format",
    color: "#98fb98",
  },

  gfm: {
    label: "Markdown",
    description: "GitHub Flavoured Markdown output format.",
    color: "#ba55d3",
  },

  javadocJar: {
    label: "Javadoc JAR",
    description: "Dokkatoo is used to create a Javadoc JAR.",
    color: "#00ffff",
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
