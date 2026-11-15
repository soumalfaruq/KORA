import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "KORA — Le forum des développeurs",
  description: "Posez vos questions techniques et partagez vos solutions avec la communauté KORA.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
