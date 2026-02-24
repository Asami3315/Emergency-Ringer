import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import logo from "figma:asset/62028d7a91b9378190f89a567675534bfe387dd8.png";

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState<"logo" | "text" | "exit">("logo");

  useEffect(() => {
    const textTimer = setTimeout(() => setPhase("text"), 800);
    const exitTimer = setTimeout(() => setPhase("exit"), 2600);
    const completeTimer = setTimeout(() => onComplete(), 3200);

    return () => {
      clearTimeout(textTimer);
      clearTimeout(exitTimer);
      clearTimeout(completeTimer);
    };
  }, [onComplete]);

  return (
    <AnimatePresence>
      {phase !== "exit" ? (
        <motion.div
          exit={{ opacity: 0, scale: 1.05 }}
          transition={{ duration: 0.5, ease: "easeInOut" }}
          className="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-gradient-to-br from-[#FFB703] via-[#FFC733] to-[#FF9500] overflow-hidden"
        >
          {/* Decorative background rings */}
          <motion.div
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 0.08, scale: 1 }}
            transition={{ duration: 1.5, ease: "easeOut" }}
            className="absolute w-[600px] h-[600px] rounded-full border-[60px] border-white"
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.3 }}
            animate={{ opacity: 0.05, scale: 1 }}
            transition={{ duration: 1.8, ease: "easeOut", delay: 0.2 }}
            className="absolute w-[900px] h-[900px] rounded-full border-[40px] border-white"
          />

          {/* Glow behind logo */}
          <motion.div
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 0.3, scale: 1 }}
            transition={{ duration: 1.2, ease: "easeOut", delay: 0.1 }}
            className="absolute w-48 h-48 rounded-full bg-white blur-3xl"
          />

          {/* Logo */}
          <motion.div
            initial={{ opacity: 0, scale: 0.3, rotate: -45 }}
            animate={{ opacity: 1, scale: 1, rotate: 0 }}
            transition={{
              duration: 0.8,
              ease: [0.175, 0.885, 0.32, 1.275],
            }}
            className="relative z-10 mb-6"
          >
            <motion.div
              animate={{
                y: [0, -6, 0],
              }}
              transition={{
                duration: 2.5,
                repeat: Infinity,
                ease: "easeInOut",
              }}
            >
              <img
                src={logo}
                alt="Company Logo"
                className="w-32 h-32 object-contain drop-shadow-2xl"
              />
            </motion.div>
          </motion.div>

          {/* App Name */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={
              phase === "text" || phase === "exit"
                ? { opacity: 1, y: 0 }
                : { opacity: 0, y: 20 }
            }
            transition={{ duration: 0.5, ease: "easeOut" }}
            className="relative z-10 text-center"
          >
            <h1 className="text-white text-[28px] tracking-tight mb-1.5 drop-shadow-sm">
              Emergency Ringer
            </h1>
            <motion.p
              initial={{ opacity: 0 }}
              animate={
                phase === "text" || phase === "exit"
                  ? { opacity: 1 }
                  : { opacity: 0 }
              }
              transition={{ duration: 0.4, delay: 0.2, ease: "easeOut" }}
              className="text-white/70 text-[14px] tracking-wide"
            >
              Always reachable when it matters
            </motion.p>
          </motion.div>

          {/* Loading dots */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={
              phase === "text" || phase === "exit"
                ? { opacity: 1 }
                : { opacity: 0 }
            }
            transition={{ delay: 0.5, duration: 0.3 }}
            className="absolute bottom-20 flex gap-2"
          >
            {[0, 1, 2].map((i) => (
              <motion.div
                key={i}
                className="w-2 h-2 rounded-full bg-white/60"
                animate={{
                  scale: [1, 1.4, 1],
                  opacity: [0.4, 1, 0.4],
                }}
                transition={{
                  duration: 0.8,
                  repeat: Infinity,
                  delay: i * 0.15,
                  ease: "easeInOut",
                }}
              />
            ))}
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
