import { createHtmlPlugin } from "vite-plugin-html";

const scalaVersion = "3.3.5";
const submodule = "mortgage-web";

export default ({ mode }) => {
  const artifact = `${mode === "production" ? "opt" : "fastopt"}`;
  const mainJS = `./target/scala-${scalaVersion}/${submodule}-${artifact}`;

  return {
    resolve: {
      alias: [
        {
          find: "@scalaJSOutput",
          replacement: mainJS,
        },
      ],
    },
    plugins: [
      createHtmlPlugin({
        minify: process.env.NODE_ENV === "production",
        template: "index.html",
        entry: "main.js",
      }),
    ],
  };
};
