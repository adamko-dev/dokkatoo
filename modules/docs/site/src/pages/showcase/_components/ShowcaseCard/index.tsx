import React from "react";
import clsx from "clsx";
import Link from "@docusaurus/Link";
import Translate from "@docusaurus/Translate";
// import Image from "@theme/IdealImage";
import FavouriteIcon from "@site/src/components/svgIcons/FavouriteIcon";
import {type Tag, TagList, Tags, type TagType, type User,} from "@site/src/data/users";
import {sortBy} from "@site/src/utils/jsUtils";
import Heading from "@theme/Heading";
import Tooltip from "../ShowcaseTooltip";
import styles from "./styles.module.css";

const TagComp = React.forwardRef<HTMLLIElement, Tag>(
    ({label, color, description}, ref) => (
        <li ref={ref} className={styles.tag} title={description}>
          <span className={styles.textLabel}>{label.toLowerCase()}</span>
          <span className={styles.colorLabel} style={{backgroundColor: color}}/>
        </li>
    ),
);

function ShowcaseCardTag({tags}: { tags: TagType[] }) {
  const tagObjects = tags.map((tag) => ({tag, ...Tags[tag]}));

  // Keep same order for all tags
  const tagObjectsSorted = sortBy(tagObjects, (tagObject) =>
      TagList.indexOf(tagObject.tag),
  );

  return (
      <>
        {tagObjectsSorted.map((tagObject, index) => {
          const id = `showcase_card_tag_${tagObject.tag}`;

          return (
              <Tooltip
                  key={index}
                  text={tagObject.description}
                  anchorEl="#__docusaurus"
                  id={id}>
                <TagComp key={index} {...tagObject} />
              </Tooltip>
          );
        })}
      </>
  );
}

function getCardImage(user: User): React.JSX.Element {
  if (user.preview) {

    // const lastIndex = user.preview.lastIndexOf(".svg");

    // if (user.preview.endsWith(".svg")) {
    //   return <svg ref={user.preview} alt={user.title}/>
    // } else {
    // }
    if (typeof user.preview == "string") {
      return <img src={user.preview} alt={user.title}/>
    } else {
      return user.preview
    }
  } else {
    return <React.Fragment/>
  }
}

function ShowcaseCard({user}: { user: User }) {
  const image = getCardImage(user);
  let websiteLink: React.JSX.Element = <div> {user.title} </div>
  if (user.website) {
    websiteLink = <Link href={user.website} className={styles.showcaseCardLink}>
      {user.title}
    </Link>
  }

  return (
      <li key={user.title} className="card shadow--md">
        <div className={clsx("card__image", styles.showcaseCardImage)}>
          {image}
        </div>
        <div className="card__body">
          <div className={clsx(styles.showcaseCardHeader)}>
            <Heading as="h4" className={styles.showcaseCardTitle}>
              {websiteLink}
            </Heading>
            {user.tags.includes("favourite") && (
                <FavouriteIcon svgClass={styles.svgIconFavourite} size="small"/>
            )}
            {user.source && (
                <Link
                    href={user.source}
                    className={clsx(
                        "button button--secondary button--sm",
                        styles.showcaseCardSrcBtn,
                    )}>
                  <Translate id="showcase.card.sourceLink">source</Translate>
                </Link>
            )}
          </div>
          <p className={styles.showcaseCardBody}>{user.description}</p>
        </div>
        <ul className={clsx("card__footer", styles.cardFooter)}>
          <ShowcaseCardTag tags={user.tags}/>
        </ul>
      </li>
  );
}

export default React.memo(ShowcaseCard);
