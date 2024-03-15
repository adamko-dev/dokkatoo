import clsx from "clsx";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import HomepageFeatures from "@site/src/components/HomepageFeatures";
import Heading from "@theme/Heading";

import styles from "./index.module.css";
import React from "react";


function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
      <header className={clsx("hero hero--primary", styles.heroBanner)}>
        <div className="container">
          <Heading as="h1" className="hero__title">
            {siteConfig.title}
          </Heading>
          <p className="hero__subtitle">{siteConfig.tagline}</p>
          <div className={styles.buttons}>
            <Link className="button button--secondary button--lg" to="/docs">
              Dive into the documentation 🏊
            </Link>
            <Link className="button button--secondary button--lg" to="/showcase">
              See the showcase 🖼️
            </Link>
            <span className={styles.indexCtasGitHubButtonWrapper}>
            <iframe
                className={styles.indexCtasGitHubButton}
                src="https://ghbtns.com/github-btn.html?user=adamko-dev&amp;repo=dokkatoo&amp;type=star&amp;count=true&amp;size=large"
                width={160}
                height={30}
                title="GitHub Stars"
            />
          </span>
          </div>
        </div>
      </header>
  );
}

export default function Home(): React.JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
      <Layout
          title={`Hello from ${siteConfig.title}`}
          description="Description will go into a meta tag in <head />">
        <HomepageHeader/>
        <main>
          <HomepageFeatures/>
        </main>
      </Layout>
  );
}
