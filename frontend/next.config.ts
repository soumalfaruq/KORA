import type { NextConfig } from "next";

const configuredApiOrigin = process.env.KORA_API_URL;
if (process.env.NODE_ENV === "production" && !configuredApiOrigin) {
  throw new Error("KORA_API_URL must be set when building for production.");
}
const apiOrigin = configuredApiOrigin ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiOrigin}/api/:path*` }];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
