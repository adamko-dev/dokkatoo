import React, {type ComponentProps, type ReactNode} from "react";
import clsx from "clsx";
import styles from "./styles.module.css";

type SvgIconProps = ComponentProps<"svg"> & {
  viewBox?: string;
  size?: "inherit" | "small" | "medium" | "large";
  color?: "inherit" | "primary" | "secondary" | "success" | "error" | "warning";
  svgClass?: string; // Class attribute on the child
  colorAttr?: string; // Applies a color attribute to the SVG element.
  children: ReactNode; // Node passed into the SVG element.
};

function SvgIcon(props: SvgIconProps): React.JSX.Element {
  const {
    svgClass,
    colorAttr,
    children,
    color = "inherit",
    size = "medium",
    viewBox = "0 0 24 24",
    ...rest
  } = props;

  return (
      <svg
          viewBox={viewBox}
          color={colorAttr}
          aria-hidden
          className={clsx(styles.svgIcon, styles[color], styles[size], svgClass)}
          {...rest}>
        {children}
      </svg>
  );
}

export function FavouriteIcon(
    props: Omit<SvgIconProps, "children">,
): React.JSX.Element {
  return (
      <SvgIcon {...props}>
        <path
            d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>
      </SvgIcon>
  );
}

export function AndroidIcon(
    props: Omit<SvgIconProps, "children">,
): React.JSX.Element {
  return (
      <SvgIcon viewBox={"0 0 16 16"} {...props}>
        <path
            d="m10.213 1.471.691-1.26q.069-.124-.048-.192-.128-.057-.195.058l-.7 1.27A4.8 4.8 0 0 0 8.005.941q-1.032 0-1.956.404l-.7-1.27Q5.281-.037 5.154.02q-.117.069-.049.193l.691 1.259a4.25 4.25 0 0 0-1.673 1.476A3.7 3.7 0 0 0 3.5 5.02h9q0-1.125-.623-2.072a4.27 4.27 0 0 0-1.664-1.476ZM6.22 3.303a.37.37 0 0 1-.267.11.35.35 0 0 1-.263-.11.37.37 0 0 1-.107-.264.37.37 0 0 1 .107-.265.35.35 0 0 1 .263-.11q.155 0 .267.11a.36.36 0 0 1 .112.265.36.36 0 0 1-.112.264m4.101 0a.35.35 0 0 1-.262.11.37.37 0 0 1-.268-.11.36.36 0 0 1-.112-.264q0-.154.112-.265a.37.37 0 0 1 .268-.11q.155 0 .262.11a.37.37 0 0 1 .107.265q0 .153-.107.264M3.5 11.77q0 .441.311.75.311.306.76.307h.758l.01 2.182q0 .414.292.703a.96.96 0 0 0 .7.288.97.97 0 0 0 .71-.288.95.95 0 0 0 .292-.703v-2.182h1.343v2.182q0 .414.292.703a.97.97 0 0 0 .71.288.97.97 0 0 0 .71-.288.95.95 0 0 0 .292-.703v-2.182h.76q.436 0 .749-.308.31-.307.311-.75V5.365h-9zm10.495-6.587a.98.98 0 0 0-.702.278.9.9 0 0 0-.293.685v4.063q0 .406.293.69a.97.97 0 0 0 .702.284q.42 0 .712-.284a.92.92 0 0 0 .293-.69V6.146a.9.9 0 0 0-.293-.685 1 1 0 0 0-.712-.278m-12.702.283a1 1 0 0 1 .712-.283q.41 0 .702.283a.9.9 0 0 1 .293.68v4.063a.93.93 0 0 1-.288.69.97.97 0 0 1-.707.284 1 1 0 0 1-.712-.284.92.92 0 0 1-.293-.69V6.146q0-.396.293-.68"/>
      </SvgIcon>
  );
}


export function KotlinIcon(
    props: Omit<SvgIconProps, "children">,
): React.JSX.Element {
  return (
      <SvgIcon viewBox={"0 0 24 24"} {...props}>
        <path d="m20.554 21.368h-20.554v-20.543h20.554l-10.489 10.119z"/>
      </SvgIcon>
  );
}
