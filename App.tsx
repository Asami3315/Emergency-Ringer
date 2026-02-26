import { useState, useCallback } from "react";
import { RouterProvider } from "react-router";
import { router } from "./routes";
import { SplashScreen } from "./components/SplashScreen";
import { IntroScreens } from "./components/IntroScreens";

type AppPhase = "splash" | "intro" | "app";

export default function App() {
  const [phase, setPhase] = useState<AppPhase>("splash");

  const handleSplashComplete = useCallback(() => {
    setPhase("intro");
  }, []);

  const handleIntroComplete = useCallback(() => {
    setPhase("app");
  }, []);

  return (
    <>
      {phase === "splash" && (
        <SplashScreen onComplete={handleSplashComplete} />
      )}
      {phase === "intro" && (
        <IntroScreens onComplete={handleIntroComplete} />
      )}
      {phase === "app" && <RouterProvider router={router} />}
    </>
  );
}
