import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { initAnalytics } from "./analytics";
import { ContentProvider } from "./content/ContentContext";
import "./styles/app.css";
import "./styles/radius.css";
import "./styles/responsive.css";
import "./styles/theme-light.css";

initAnalytics();

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ContentProvider>
      <App />
    </ContentProvider>
  </React.StrictMode>
);
