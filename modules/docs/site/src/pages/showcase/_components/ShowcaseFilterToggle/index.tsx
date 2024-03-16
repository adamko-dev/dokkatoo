import React, {useCallback, useEffect, useState} from "react";
import clsx from "clsx";
import {useHistory, useLocation} from "@docusaurus/router";

import styles from "./styles.module.css";
import {prepareUserState} from "@site/src/pages/showcase";

export type Operator = "OR" | "AND";

export const OperatorQueryKey = "operator";

export function readOperator(search: string): Operator {
  return (new URLSearchParams(search).get(OperatorQueryKey) ??
          "OR") as Operator;
}

export default function ShowcaseFilterToggle(): React.JSX.Element {
  const id = "showcase_filter_toggle";
  const location = useLocation();
  const history = useHistory();
  const [operator, setOperator] = useState(false);
  useEffect(() => {
    setOperator(readOperator(location.search) === "AND");
  }, [location]);
  const toggleOperator = useCallback(() => {
    setOperator((o) => !o);
    const searchParams = new URLSearchParams(location.search);
    searchParams.delete(OperatorQueryKey);
    if (!operator) {
      searchParams.append(OperatorQueryKey, "AND");
    }
    history.push({
      ...location,
      search: searchParams.toString(),
      state: prepareUserState(),
    });
  }, [operator, location, history]);

  const ClearTag = () => {
    history.push({
      ...location,
      search: "",
      state: prepareUserState(),
    });
  };

  return (
      <div className="row" style={{alignItems: "center"}}>
        <label htmlFor={id} className={clsx(styles.checkboxLabel, "shadow--md")}>
          <span className={styles.checkboxLabelOr}>OR</span>
          <span className={styles.checkboxLabelAnd}>AND</span>
        </label>

        <button
            className="button button--outline button--primary"
            type="button"
            onClick={() => ClearTag()}>
          Clear All
        </button>
      </div>
  );
}
