import React, {type ComponentProps, type ReactElement, useEffect, useMemo, useState} from "react";
import clsx from "clsx";
import ExecutionEnvironment from "@docusaurus/ExecutionEnvironment";
import Translate, {translate} from "@docusaurus/Translate";
import {useHistory, useLocation} from "@docusaurus/router";
import {usePluralForm} from "@docusaurus/theme-common";

import Link from "@docusaurus/Link";
import Layout from "@theme/Layout";
import FavouriteIcon from '@site/src/components/svgIcons/FavouriteIcon';
import {sortedUsers, TagList, Tags, type TagType, type User,} from "@site/src/data/users";
import Heading from "@theme/Heading";
import ShowcaseTagSelect, {readSearchTags,} from "./_components/ShowcaseTagSelect";
import ShowcaseCard from "./_components/ShowcaseCard";
import ShowcaseTooltip from "./_components/ShowcaseTooltip";

import styles from "./styles.module.css";

const TITLE = "Dokkatoo Showcase";
const DESCRIPTION = "List of projects using Dokkatoo people are building with Docusaurus";

type UserState = {
  scrollTopPosition: number;
  focusedElementId: string | undefined;
};

function restoreUserState(userState: UserState | null) {
  const {scrollTopPosition, focusedElementId} = userState ?? {
    scrollTopPosition: 0,
    focusedElementId: undefined,
  };
  if(focusedElementId) document.getElementById(focusedElementId)?.focus();
  window.scrollTo({top: scrollTopPosition});
}

export function prepareUserState(): UserState | undefined {
  if (ExecutionEnvironment.canUseDOM) {
    return {
      scrollTopPosition: window.scrollY,
      focusedElementId: document.activeElement?.id,
    };
  }

  return undefined;
}

const SearchNameQueryKey = "name";

function readSearchName(search: string) {
  return new URLSearchParams(search).get(SearchNameQueryKey);
}

function filterUsers(
    users: User[],
    selectedTags: TagType[],
    searchName: string | null,
) {
  if (searchName) {
    // eslint-disable-next-line no-param-reassign
    users = users.filter((user) =>
        user.title.toLowerCase().includes(searchName.toLowerCase()),
    );
  }
  if (selectedTags.length === 0) {
    return users;
  }
  return users.filter((user) => {
    if (user.tags.length === 0) {
      return false;
    }
    return selectedTags.every((tag) => user.tags.includes(tag));
  });
}

function useFilteredUsers() {
  const location = useLocation<UserState>();
  // On SSR / first mount (hydration) no tag is selected
  const [selectedTags, setSelectedTags] = useState<TagType[]>([]);
  const [searchName, setSearchName] = useState<string | null>(null);
  // Sync tags from QS to state (delayed on purpose to avoid SSR/Client
  // hydration mismatch)
  useEffect(() => {
    setSelectedTags(readSearchTags(location.search));
    setSearchName(readSearchName(location.search));
    restoreUserState(location.state);
  }, [location]);

  return useMemo(
      () => filterUsers(sortedUsers, selectedTags, searchName),
      [selectedTags, searchName],
  );
}

function ShowcaseHeader() {
  return (
      <section className="margin-top--lg margin-bottom--lg text--center">
        <Heading as="h1">{TITLE}</Heading>
        <p>{DESCRIPTION}</p>
        <Link className="button button--secondary" to="https://github.com/adamko-dev/dokkatoo/issues/new">
          🙏 Please add your site
        </Link>
      </section>
  );
}

function useSiteCountPlural() {
  const {selectMessage} = usePluralForm();
  return (sitesCount: number) =>
      selectMessage(
          sitesCount,
          translate(
              {
                id: "showcase.filters.resultCount",
                description:
                    "Pluralized label for the number of sites found on the showcase. Use as much plural forms (separated by \"|\") as your language support (see https://www.unicode.org/cldr/cldr-aux/charts/34/supplemental/language_plural_rules.html)",
                message: "1 site|{sitesCount} sites",
              },
              {sitesCount},
          ),
      );
}

function ShowcaseFilters() {
  const filteredUsers = useFilteredUsers();
  const siteCountPlural = useSiteCountPlural();
  return (
      <section className="container margin-top--l margin-bottom--lg">
        <div className={clsx("margin-bottom--sm", styles.filterCheckbox)}>
          <div>
            <Heading as="h2">
              Filters
            </Heading>
            <span>{siteCountPlural(filteredUsers.length)}</span>
          </div>
        </div>
        <ul className={clsx("clean-list", styles.checkboxList)}>
          {TagList.map((tag, i) => {
            const {label, description, color} = Tags[tag];
            const id = `showcase_checkbox_id_${tag}`;
            let icon: ReactElement<ComponentProps<"svg">>
            if (tag === "favourite") {
              icon = <FavouriteIcon svgClass={styles.svgIconFavouriteXs}/>
            } else {
              icon =  <span
                  style={{
                    backgroundColor: color,
                    width: 10,
                    height: 10,
                    borderRadius: "50%",
                    marginLeft: 8,
                  }}
              />
            }
            return (
                <li key={i} className={styles.checkboxListItem}>
                  <ShowcaseTooltip
                      id={id}
                      text={description}
                      anchorEl="#__docusaurus">
                    <ShowcaseTagSelect
                        tag={tag}
                        id={id}
                        label={label}
                        icon={icon}
                    />
                  </ShowcaseTooltip>
                </li>
            );
          })}
        </ul>
      </section>
  );
}

const favouriteUsers = sortedUsers.filter((user) =>
    user.tags.includes("favourite"),
);
const otherUsers = sortedUsers.filter(
    (user) => !user.tags.includes("favourite"),
);

function SearchBar() {
  const history = useHistory();
  const location = useLocation();
  const [value, setValue] = useState<string | null>(null);
  useEffect(() => {
    setValue(readSearchName(location.search));
  }, [location]);
  return (
      <div className={styles.searchContainer}>
        <input
            id="searchbar"
            placeholder={translate({
              message: "Search for site name...",
              id: "showcase.searchBar.placeholder",
            })}
            value={value ?? undefined}
            onInput={(e) => {
              setValue(e.currentTarget.value);
              const newSearch = new URLSearchParams(location.search);
              newSearch.delete(SearchNameQueryKey);
              if (e.currentTarget.value) {
                newSearch.set(SearchNameQueryKey, e.currentTarget.value);
              }
              history.push({
                ...location,
                search: newSearch.toString(),
                state: prepareUserState(),
              });
              setTimeout(() => {
                document.getElementById("searchbar")?.focus();
              }, 0);
            }}
        />
      </div>
  );
}

function ShowcaseCards() {
  const filteredUsers = useFilteredUsers();

  if (filteredUsers.length === 0) {
    return (
        <section className="margin-top--lg margin-bottom--xl">
          <div className="container padding-vert--md text--center">
            <Heading as="h2">
              <Translate id="showcase.usersList.noResult">No result</Translate>
            </Heading>
          </div>
        </section>
    );
  }

  return (
      <section className="margin-top--lg margin-bottom--xl">
        {filteredUsers.length === sortedUsers.length ? (
            <>
              <div className={styles.showcaseFavourite}>
                <div className="container">
                  <div className={clsx("margin-bottom--md", styles.showcaseFavouriteHeader)}>
                    <Heading as="h2">
                      Our favourites
                    </Heading>
                    <FavouriteIcon svgClass={styles.svgIconFavourite}/>
                  </div>
                  <ul className={clsx("container", "clean-list", styles.showcaseList)}>
                    {favouriteUsers.map((user) => (
                        <ShowcaseCard key={user.title} user={user}/>
                    ))}
                  </ul>
                </div>
              </div>
              <div className="container margin-top--lg">
                <Heading as="h2" className={styles.showcaseHeader}>
                  All sites
                </Heading>
                <ul className={clsx("clean-list", styles.showcaseList)}>
                  {otherUsers.map((user) => (
                      <ShowcaseCard key={user.title} user={user}/>
                  ))}
                </ul>
              </div>
            </>
        ) : (
             <div className="container">
               <div
                   className={clsx("margin-bottom--md", styles.showcaseFavouriteHeader)}
               />
               <ul className={clsx("clean-list", styles.showcaseList)}>
                 {filteredUsers.map((user) => (
                     <ShowcaseCard key={user.title} user={user}/>
                 ))}
               </ul>
             </div>
         )}
      </section>
  );
}

export default function Showcase(): React.JSX.Element {
  return (
      <Layout title={TITLE} description={DESCRIPTION}>
        <main className="margin-vert--lg">
          <ShowcaseHeader/>
          <ShowcaseFilters/>
          <div style={{display: "flex", marginLeft: "auto"}} className="container">
            <SearchBar/>
          </div>
          <ShowcaseCards/>
        </main>
      </Layout>
  );
}
