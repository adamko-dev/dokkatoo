import React from "react";
import clsx from "clsx";
import styles from "./styles.module.css";
import Heading from "@theme/Heading";
import UndrawArtMuseum from "/static/img/feature-icons/undraw_art_museum.svg";

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<"svg">>;
  description: React.JSX.Element;
};


const FeatureList: FeatureItem[] = [
  {
    title: "Easy to Use",
    Svg: require("@site/static/img/feature-icons/undraw_setup_wizard.svg").default,
    description: (
        <>
          Quickly add Dokkatoo into existing Kotlin Gradle projects, and generate comprehensive
          documentation effortlessly.
        </>
    ),
  },
  {
    title: "Powered by Gradle",
    Svg: require("@site/static/img/feature-icons/undraw_my_universe.svg").default,
    description: (
        <>
          Engineered with Gradle best practices in mind, Dokkatoo operates seamlessly and
          efficiently within your Gradle environment.
        </>
    ),
  },
  {
    title: "Versatile Output",
    Svg: UndrawArtMuseum,
    description: (
        <>
          Produce documentation in various formats including HTML, Markdown, Javadoc, and Jekyll.
        </>
    ),
  },
  {
    title: "Extensibility",
    Svg: require("@site/static/img/feature-icons/undraw_under_construction.svg").default,
    description: (
        <>
          Customize Dokkatoo to suit your requirements with its open architecture.
          Add your own plugins to enhance functionality.
        </>
    ),
  },
  {
    title: "Community-driven Development",
    Svg: require("@site/static/img/feature-icons/undraw_open_source.svg").default,
    description: (
        <>
          Dokkatoo is developed openly under the Apache-2.0 license, inviting collaboration and
          ensuring transparency.
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
