import type { CodegenConfig } from "@graphql-codegen/cli";
const config: CodegenConfig = {
  schema: "../src/main/resources/graphql/schema.graphqls",
  documents: ["src/operations.graphql"],
  generates: {
    "src/generated.ts": {
      plugins: ["typescript-operations", "typed-document-node"],
      config: {
        enumsAsTypes: true,
        scalars: { ID: "string" },
        avoidOptionals: true,
      },
    },
  },
};
export default config;
