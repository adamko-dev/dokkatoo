import React from "react";
import clsx from "clsx";
// import Link from '@docusaurus/Link';
import styles from "./styles.module.css";
import Heading from "@theme/Heading";
import UndrawArtMuseum from "/static/img/feature-icons/undraw_art_museum.svg";

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<"svg">>;
  description: React.JSX.Element;
};


// TODO update features text to be better, less vague
const FeatureList: FeatureItem[] = [
  {
    title: "Easy to Use",
    Svg: require("@site/static/img/feature-icons/undraw_setup_wizard.svg").default,
    description: (
        <>
          Drop Dokkatoo into an existing Kotlin Gradle plugin, and immediately generate
          documentation.
        </>
    ),
  },
  {
    title: "Powered by Gradle",
    Svg: require("@site/static/img/feature-icons/undraw_my_universe.svg").default,
    description: (
        <>
          Completely compatible with Gradle best practices, Dokkatoo works fast.
        </>
    ),
  },
  {
    title: "Multiple output formats",
    Svg: UndrawArtMuseum,
    description: (
        <>
          Dokkatoo can generate HTML, Markdown, Javadoc, and Jeykll
        </>
    ),
  },
  {
    title: "Hackable",
    Svg: require("@site/static/img/feature-icons/undraw_under_construction.svg").default,
    description: (
        <>
          Dokkatoo is open for extension, you can drop in your own plugins.
        </>
    ),
  },
  {
    title: "Open source",
    Svg: require("@site/static/img/feature-icons/undraw_open_source.svg").default,
    description: (
        <>
          Dokkatoo is open for extension, you can drop in your own plugins.
        </>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
      <div className={clsx("col margin--xs padding-sm", styles.col)}>
        <div className="text--center padding-vert--sm">
          <Svg className={clsx("", styles.featureSvg)} role="img"/>
        </div>
        <div className="text--center padding-horiz--md">
          <Heading as="h3">{title}</Heading>
          <p>{description}</p>
        </div>
      </div>
  );
}

export default function HomepageFeatures(): React.JSX.Element {
  return (
      <section className={styles.features}>
        <div className="container">
          <div className="row">
            {FeatureList.map((props, idx) => (
                <Feature key={idx} {...props} />
            ))}
          </div>
        </div>
      </section>
  );
}
