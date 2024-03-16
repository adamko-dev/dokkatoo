import React, {useState, useEffect, useCallback} from 'react';
import clsx from 'clsx';
import {useHistory, useLocation} from '@docusaurus/router';

import {prepareUserState} from '../../index';

import styles from './styles.module.css';

export type Operator = 'OR' | 'AND';

export const OperatorQueryKey = 'operator';

export function readOperator(search: string): Operator {
  return (new URLSearchParams(search).get(OperatorQueryKey) ??
          'OR') as Operator;
}

export default function ShowcaseFilterToggle(): JSX.Element {
  const id = 'showcase_filter_toggle';
  const location = useLocation();
  const history = useHistory();
  const [operator, setOperator] = useState(false);
  useEffect(() => {
    setOperator(readOperator(location.search) === 'AND');
  }, [location]);
  const toggleOperator = useCallback(() => {
    setOperator((o) => !o);
    const searchParams = new URLSearchParams(location.search);
    searchParams.delete(OperatorQueryKey);
    if (!operator) {
      searchParams.append(OperatorQueryKey, 'AND');
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
      search: '',
      state: prepareUserState(),
    });
  };

  return (
      <div className="row" style={{alignItems: 'center'}}>
        <input
            type="checkbox"
            id={id}
            className="screen-reader-only"
            aria-label="Toggle between or and and for the tags you selected"
            onChange={toggleOperator}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                toggleOperator();
              }
            }}
            checked={operator}
        />
        <label htmlFor={id} className={clsx(styles.checkboxLabel, 'shadow--md')}>
          {/* eslint-disable @docusaurus/no-untranslated-text */}
          <span className={styles.checkboxLabelOr}>OR</span>
          <span className={styles.checkboxLabelAnd}>AND</span>
          {/* eslint-enable @docusaurus/no-untranslated-text */}
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
