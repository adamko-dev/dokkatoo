import {Config} from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const config: Config = {
  title: "Dokkatoo",
  tagline: "Generate Documentation for your Kotlin Projects",
  url: "https://adamko-dev.github.io",
  baseUrl: "/dokkatoo",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  favicon: "img/logo-icon.svg",

  // GitHub pages deployment config
  organizationName: "adamko-dev",
  projectName: "dokkatoo",

  i18n: {defaultLocale: "en", locales: ["en"]},

  presets: [
    [
      "classic", {
      docs: {
        sidebarPath: require.resolve("./sidebars.config.js"),
        sidebarCollapsible: false,
        editUrl: "https://github.com/adamko-dev/dokkatoo/blob/main/modules/docs/site/",
      },
      theme: {
        customCss: [
          require.resolve("./src/css/custom.css"),
        ],
      },
    } satisfies Preset.Options,
    ],
  ],

  // scripts: [],

  clientModules: [
    require.resolve("./src/css/global.scss"),
  ],

  themeConfig: {
    colorMode: {
      defaultMode: "dark",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    metadata: [{
      name: "keywords",
      content: "kotlin, dokka, gradle, plugin, dokkatoo, documentation, api reference, html, javadoc"
    }],
    navbar: {
      title: "Dokkatoo",
      logo: {alt: "Dokkatoo logo", src: "img/logo-icon.svg"},
      items: [
        // {to: "/getting-started", label: "Getting started", position: "left"},
        {
          type: "doc",
          docId: "getting-started",
          label: "Documentation",
          position: "left",
        },
        {
          label: "Showcase",
          position: "left",
          href: "/dokkatoo/showcase",
        },
        {
          label: "API Reference",
          position: "left",
          className: "external-link",
          to: "pathname:///dokkatoo/kdoc/index.html",
        },
        {
          label: "Releases",
          position: "left",
          href: "https://github.com/adamko-dev/dokkatoo/releases",
        },
        {
          href: "https://github.com/adamko-dev/dokkatoo",
          className: "header-github-link",
          "aria-label": "GitHub repository",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Discover",
          items: [
            {label: "Documentation", to: "/docs"},
            {label: "Showcase", to: "/showcase"},
            {label: "API Reference", to: "pathname:///dokkatoo/kdoc/index.html"},
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "GitHub Homepage",
              href: "https://github.com/adamko-dev/dokkatoo/",
            },
            {
              label: "Kotlin Slack #dokka",
              href: "https://slack-chats.kotlinlang.org/c/dokka/",
            },
            {
              label: "Help and Discussions",
              href: "https://github.com/adamko-dev/dokkatoo/discussions",
            },
            {
              label: "Issues and requests",
              href: "https://github.com/adamko-dev/dokkatoo/issues",
            },
          ]
        },
        {
          title: "Releases",
          items: [
            {
              label: "GitHub Releases",
              href: "https://github.com/adamko-dev/dokkatoo/releases",
            },
            {
              label: "Gradle Plugin Portal",
              href: "https://plugins.gradle.org/search?term=dokkatoo",
            },
            {
              label: "Maven Central",
              href: "https://search.maven.org/search?q=g:dev.adamko.dokkatoo",
            },
            {
              label: "Maven Central Snapshots",
              href: "https://s01.oss.sonatype.org/content/repositories/snapshots/dev/adamko/dokkatoo/dokkatoo-plugin",
            },
          ]
        }
      ],
      copyright: `Copyright © 2023`,
    },
    prism: {
      // themes are managed by global.scss
      theme: {plain: {}, styles: []},
      darkTheme: {plain: {}, styles: []},
      additionalLanguages: ["kotlin", "groovy", "markup", "bash"],
    },
  } satisfies Preset.ThemeConfig,

  plugins: ["docusaurus-plugin-sass"],
};

module.exports = config;
