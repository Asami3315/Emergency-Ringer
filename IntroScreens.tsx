import { useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Shield, Users, ChevronRight } from "lucide-react";
import {
  PhonePersonIllustration,
  ProtectedCircleIllustration,
} from "./IntroIllustrations";

interface IntroScreensProps {
  onComplete: () => void;
}

const screens = [
  {
    icon: Shield,
    color: "#FFB703",
    bgGradient: "from-[#FFF8E7] to-[#FFF1CC]",
    title: "Never Miss an Emergency",
    description:
      "Emergency Ringer ensures critical calls from your trusted contacts always ring loud — even when your phone is on silent or Do Not Disturb.",
    Illustration: PhonePersonIllustration,
  },
  {
    icon: Users,
    color: "#E0C5F5",
    bgGradient: "from-[#F5F0FF] to-[#EDE4FF]",
    title: "Your Circle, Protected",
    description:
      "Add your emergency contacts — family, doctors, close friends — and customize ringtone, volume, and alerts. Stay connected when it truly matters.",
    Illustration: ProtectedCircleIllustration,
  },
];

export function IntroScreens({ onComplete }: IntroScreensProps) {
  const [currentScreen, setCurrentScreen] = useState(0);
  const [direction, setDirection] = useState(1);

  const goNext = () => {
    if (currentScreen < screens.length - 1) {
      setDirection(1);
      setCurrentScreen((prev) => prev + 1);
    }
  };

  const screen = screens[currentScreen];
  const isLastScreen = currentScreen === screens.length - 1;
  const IllustrationComponent = screen.Illustration;

  const variants = {
    enter: (dir: number) => ({
      x: dir > 0 ? 300 : -300,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (dir: number) => ({
      x: dir > 0 ? -300 : 300,
      opacity: 0,
    }),
  };

  return (
    <div className="fixed inset-0 z-[90] flex items-center justify-center bg-[#F5F3EF]">
      <div className="w-full max-w-[430px] h-full mx-auto flex flex-col overflow-hidden relative">
        <AnimatePresence mode="wait" custom={direction}>
          <motion.div
            key={currentScreen}
            custom={direction}
            variants={variants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] }}
            className="flex-1 flex flex-col"
          >
            {/* Illustration Section */}
            <div
              className={`relative w-full h-[55%] bg-gradient-to-b ${screen.bgGradient} flex items-center justify-center overflow-hidden`}
            >
              {/* Decorative background circles */}
              <motion.div
                initial={{ opacity: 0, scale: 0.5 }}
                animate={{ opacity: 0.1, scale: 1 }}
                transition={{ duration: 0.8, delay: 0.2 }}
                className="absolute w-72 h-72 rounded-full"
                style={{ backgroundColor: screen.color }}
              />
              <motion.div
                initial={{ opacity: 0, scale: 0.3 }}
                animate={{ opacity: 0.05, scale: 1 }}
                transition={{ duration: 1, delay: 0.4 }}
                className="absolute w-[400px] h-[400px] rounded-full"
                style={{ backgroundColor: screen.color }}
              />

              {/* Illustration */}
              <motion.div
                initial={{ opacity: 0, scale: 0.85 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.6, delay: 0.15, ease: "easeOut" }}
                className="relative z-10 w-72 h-72"
              >
                <IllustrationComponent />
              </motion.div>
            </div>

            {/* Content Section */}
            <div className="flex-1 bg-[#F5F3EF] px-8 pt-8 pb-6 flex flex-col">
              <motion.h2
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.25 }}
                className="text-[#2D2D2D] text-[26px] text-center mb-3"
                style={{ fontFamily: "Nunito, sans-serif" }}
              >
                {screen.title}
              </motion.h2>
              <motion.p
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.35 }}
                className="text-[#8A8A8A] text-[15px] text-center leading-relaxed px-2"
                style={{ fontFamily: "Nunito, sans-serif" }}
              >
                {screen.description}
              </motion.p>
            </div>
          </motion.div>
        </AnimatePresence>

        {/* Bottom Controls — always visible */}
        <div className="absolute bottom-0 left-0 right-0 px-8 pb-10 pt-4 bg-gradient-to-t from-[#F5F3EF] via-[#F5F3EF] to-transparent">
          {/* Page Indicators */}
          <div className="flex items-center justify-center gap-2 mb-6">
            {screens.map((_, idx) => (
              <motion.div
                key={idx}
                animate={{
                  width: idx === currentScreen ? 28 : 8,
                  backgroundColor:
                    idx === currentScreen ? "#FFB703" : "#D9D5CE",
                }}
                transition={{ duration: 0.3 }}
                className="h-2 rounded-full"
              />
            ))}
          </div>

          {/* Buttons */}
          {!isLastScreen ? (
            <motion.button
              whileTap={{ scale: 0.97 }}
              onClick={goNext}
              className="w-full h-14 rounded-[20px] bg-[#FFB703] text-white text-[16px] flex items-center justify-center gap-2 shadow-sm active:shadow-none transition-shadow"
              style={{ fontFamily: "Nunito, sans-serif" }}
            >
              Next
              <ChevronRight className="w-5 h-5" />
            </motion.button>
          ) : (
            <motion.button
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              whileTap={{ scale: 0.97 }}
              onClick={onComplete}
              className="w-full h-14 rounded-[20px] bg-[#FFB703] text-white text-[16px] flex items-center justify-center gap-2 shadow-sm active:shadow-none transition-shadow"
              style={{ fontFamily: "Nunito, sans-serif" }}
            >
              Get Started
              <ChevronRight className="w-5 h-5" />
            </motion.button>
          )}

          {/* Skip on second screen */}
          {isLastScreen && (
            <motion.button
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              onClick={onComplete}
              className="w-full mt-3 py-2 text-[#8A8A8A] text-[14px] text-center"
              style={{ fontFamily: "Nunito, sans-serif" }}
            >
              Skip for now
            </motion.button>
          )}
        </div>
      </div>
    </div>
  );
}
