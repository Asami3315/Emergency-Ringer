import { motion } from "motion/react";

export function PhonePersonIllustration() {
  return (
    <svg
      viewBox="0 0 360 360"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="w-full h-full"
    >
      {/* Background decorative blob */}
      <ellipse cx="180" cy="185" rx="140" ry="135" fill="#FFF3D6" opacity="0.6" />

      {/* Floor shadow - breathes with character */}
      <motion.ellipse
        cx="180" cy="320" rx="100" ry="10" fill="#E8DFD0" opacity="0.5"
        animate={{ rx: [100, 105, 100] }}
        transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
      />

      {/* === LEGS (static base) === */}
      {/* Left leg */}
      <path
        d="M140 270 C135 285 128 300 120 310 C118 313 122 316 126 314 C135 308 142 295 148 278 Z"
        fill="#3D5A80"
      />
      {/* Left shoe - subtle tap */}
      <motion.path
        d="M118 310 C114 312 110 314 108 312 C106 310 110 306 116 308 L120 310 Z"
        fill="#2D2D2D"
        animate={{ rotate: [0, -3, 0, 3, 0] }}
        transition={{ duration: 2.5, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "118px 310px" }}
      />

      {/* Right leg */}
      <path
        d="M170 268 C178 285 190 300 202 308 C205 310 208 307 206 304 C196 292 186 278 178 265 Z"
        fill="#3D5A80"
      />
      {/* Right shoe */}
      <path
        d="M202 308 C206 310 212 312 214 310 C216 308 212 304 206 306 L202 308 Z"
        fill="#2D2D2D"
      />

      {/* Chair/bean bag */}
      <path
        d="M110 230 C105 250 108 270 120 280 C135 290 175 290 190 280 C202 270 205 250 200 230 C195 215 115 215 110 230 Z"
        fill="#FFB703"
        opacity="0.15"
      />

      {/* === BODY - subtle breathing === */}
      <motion.g
        animate={{ scaleY: [1, 1.008, 1], y: [0, -1, 0] }}
        transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "155px 230px" }}
      >
        {/* Torso */}
        <path
          d="M135 190 C130 210 128 240 132 268 L178 268 C182 240 180 210 175 190 Z"
          fill="#FFB703"
        />
        {/* Hoodie center line */}
        <line x1="155" y1="195" x2="155" y2="265" stroke="#E5A003" strokeWidth="1.5" opacity="0.5" />
        {/* Hoodie pocket */}
        <path d="M138 235 C140 240 170 240 172 235" stroke="#E5A003" strokeWidth="1.5" fill="none" opacity="0.5" />
        {/* Collar */}
        <path d="M140 192 C145 200 165 200 170 192" stroke="#E5A003" strokeWidth="2" fill="none" />
      </motion.g>

      {/* Left arm - gentle wave/sway */}
      <motion.g
        animate={{ rotate: [0, -4, 0, 3, 0] }}
        transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "135px 205px" }}
      >
        <path
          d="M135 205 C120 218 112 235 115 252 C116 258 118 260 122 258"
          stroke="#FFDBA4" strokeWidth="14" strokeLinecap="round" fill="none"
        />
        <path
          d="M135 200 C128 210 122 220 120 228"
          stroke="#FFB703" strokeWidth="16" strokeLinecap="round" fill="none"
        />
        {/* Hand - fingers wiggle */}
        <motion.g
          animate={{ rotate: [0, 5, -5, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
          style={{ transformOrigin: "120px 256px" }}
        >
          <ellipse cx="120" cy="256" rx="7" ry="8" fill="#FFDBA4" />
          {/* Fingers */}
          <ellipse cx="114" cy="252" rx="2.5" ry="4" fill="#FFDBA4" transform="rotate(-15 114 252)" />
          <ellipse cx="126" cy="252" rx="2.5" ry="4" fill="#FFDBA4" transform="rotate(15 126 252)" />
        </motion.g>
      </motion.g>

      {/* Right arm + phone - subtle show-off movement */}
      <motion.g
        animate={{ rotate: [0, 3, 0, -2, 0] }}
        transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "175px 200px" }}
      >
        <path
          d="M175 200 C188 195 198 180 205 160 C208 152 210 145 210 140"
          stroke="#FFDBA4" strokeWidth="13" strokeLinecap="round" fill="none"
        />
        <path
          d="M175 200 C185 196 192 188 196 178"
          stroke="#FFB703" strokeWidth="16" strokeLinecap="round" fill="none"
        />
        <ellipse cx="210" cy="138" rx="8" ry="9" fill="#FFDBA4" />
        <ellipse cx="204" cy="148" rx="4" ry="5" fill="#FFDBA4" />

        {/* Phone - slight tilt with arm */}
        <rect x="196" y="100" width="32" height="58" rx="7" fill="#2D2D2D" />
        <rect x="199" y="105" width="26" height="46" rx="4" fill="#E8F4FD" />
        {/* Caller screen */}
        <circle cx="212" cy="118" r="8" fill="#B8E6C8" opacity="0.5" />
        <circle cx="212" cy="118" r="5" fill="#FFB703" />
        <circle cx="212" cy="116" r="2.5" fill="white" />
        <path d="M208 122 C208 119 216 119 216 122" fill="white" />
        <rect x="204" y="128" width="16" height="2" rx="1" fill="#CCC" />
        <rect x="207" y="132" width="10" height="1.5" rx="0.75" fill="#DDD" />
        {/* Accept/Decline buttons */}
        <circle cx="206" cy="142" r="4" fill="#FF6B6B" />
        <circle cx="218" cy="142" r="4" fill="#4ADE80" />
        <path d="M204.5 141 L207.5 143" stroke="white" strokeWidth="1.2" strokeLinecap="round" />
        <path d="M207.5 141 L204.5 143" stroke="white" strokeWidth="1.2" strokeLinecap="round" />
        <path d="M216 142 C217 140.5 219 140.5 220 142" stroke="white" strokeWidth="1.2" strokeLinecap="round" fill="none" />

        {/* Screen glow pulse */}
        <motion.rect
          x="199" y="105" width="26" height="46" rx="4"
          fill="#4A90D9" opacity="0.15"
          animate={{ opacity: [0.05, 0.2, 0.05] }}
          transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
        />
      </motion.g>

      {/* Sound waves */}
      <motion.path
        d="M234 115 C240 108 242 132 234 125"
        stroke="#FFB703" strokeWidth="2.5" strokeLinecap="round" fill="none"
        animate={{ opacity: [0, 1, 0], x: [0, 4, 8] }}
        transition={{ duration: 1.2, repeat: Infinity }}
      />
      <motion.path
        d="M242 108 C252 98 255 140 242 132"
        stroke="#FFB703" strokeWidth="2" strokeLinecap="round" fill="none"
        animate={{ opacity: [0, 0.7, 0], x: [0, 5, 10] }}
        transition={{ duration: 1.2, repeat: Infinity, delay: 0.25 }}
      />
      <motion.path
        d="M250 100 C264 88 268 150 250 140"
        stroke="#FFB703" strokeWidth="1.5" strokeLinecap="round" fill="none"
        animate={{ opacity: [0, 0.4, 0], x: [0, 6, 12] }}
        transition={{ duration: 1.2, repeat: Infinity, delay: 0.5 }}
      />

      {/* === HEAD - gentle bob/tilt === */}
      <motion.g
        animate={{ rotate: [0, 2, 0, -2, 0], y: [0, -2, 0] }}
        transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "155px 185px" }}
      >
        {/* Neck */}
        <rect x="148" y="185" width="14" height="10" fill="#FFDBA4" />

        {/* Head shape */}
        <ellipse cx="155" cy="165" rx="28" ry="30" fill="#FFDBA4" />

        {/* Hair */}
        <path
          d="M127 158 C125 135 138 118 155 118 C172 118 185 135 183 158 C183 148 178 132 168 126 C160 122 150 122 142 126 C132 132 127 148 127 158 Z"
          fill="#3D2B1F"
        />
        <path d="M130 150 C128 135 140 120 155 118 C170 120 182 135 180 150" fill="#3D2B1F" />
        <path d="M127 158 C125 165 126 170 129 170 C130 167 129 162 127 158 Z" fill="#3D2B1F" />
        <path d="M183 158 C185 165 184 170 181 170 C180 167 181 162 183 158 Z" fill="#3D2B1F" />
        <path d="M140 130 C145 122 158 118 168 122 C162 118 148 117 140 125 Z" fill="#4A3728" />

        {/* Ears */}
        <ellipse cx="127" cy="165" rx="5" ry="7" fill="#F0C890" />
        <ellipse cx="183" cy="165" rx="5" ry="7" fill="#F0C890" />

        {/* Eyes - BLINKING */}
        <motion.g
          animate={{ scaleY: [1, 1, 0.1, 1, 1, 1, 1, 1, 0.1, 1] }}
          transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "155px 164px" }}
        >
          <ellipse cx="145" cy="164" rx="4" ry="4.5" fill="white" />
          <ellipse cx="165" cy="164" rx="4" ry="4.5" fill="white" />
          {/* Pupils - looking toward phone */}
          <motion.ellipse
            cx="147" cy="164" rx="2.5" ry="3" fill="#2D2D2D"
            animate={{ cx: [147, 148, 147, 146, 147] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.ellipse
            cx="167" cy="164" rx="2.5" ry="3" fill="#2D2D2D"
            animate={{ cx: [167, 168, 167, 166, 167] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          {/* Eye shine */}
          <circle cx="148" cy="163" r="1" fill="white" />
          <circle cx="168" cy="163" r="1" fill="white" />
        </motion.g>

        {/* Eyebrows - subtle raise */}
        <motion.g
          animate={{ y: [0, -1.5, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
        >
          <path d="M138 157 C141 154 149 154 152 157" stroke="#3D2B1F" strokeWidth="2.5" strokeLinecap="round" fill="none" />
          <path d="M158 157 C161 154 169 154 172 157" stroke="#3D2B1F" strokeWidth="2.5" strokeLinecap="round" fill="none" />
        </motion.g>

        {/* Nose */}
        <path d="M153 170 C155 172 157 172 158 170" stroke="#E8B888" strokeWidth="1.5" strokeLinecap="round" fill="none" />

        {/* Mouth - talking/smiling animation */}
        <motion.path
          d="M146 178 C150 183 160 183 164 178"
          stroke="#2D2D2D"
          strokeWidth="2.5"
          strokeLinecap="round"
          fill="none"
          animate={{
            d: [
              "M146 178 C150 183 160 183 164 178",
              "M147 178 C150 181 160 181 163 178",
              "M146 178 C150 184 160 184 164 178",
              "M146 178 C150 183 160 183 164 178",
            ],
          }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        />

        {/* Blush */}
        <motion.ellipse cx="138" cy="174" rx="6" ry="3.5" fill="#FFCBA4"
          animate={{ opacity: [0.35, 0.55, 0.35] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.ellipse cx="172" cy="174" rx="6" ry="3.5" fill="#FFCBA4"
          animate={{ opacity: [0.35, 0.55, 0.35] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        />
      </motion.g>

      {/* === FLOATING ELEMENTS === */}

      {/* Shield badge - bobbing and tilting */}
      <motion.g
        animate={{ y: [-4, 6, -4], rotate: [-5, 5, -5] }}
        transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "72px 130px" }}
      >
        <circle cx="72" cy="130" r="22" fill="white" />
        <path d="M62 126 L72 118 L82 126 L82 140 C82 146 72 152 72 152 C72 152 62 146 62 140 Z" fill="#4ADE80" />
        <path d="M67 134 L70 137 L78 128" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
      </motion.g>

      {/* Bell notification - ringing animation */}
      <motion.g
        animate={{ y: [3, -5, 3], rotate: [5, -5, 5] }}
        transition={{ duration: 2.8, repeat: Infinity, ease: "easeInOut", delay: 0.3 }}
        style={{ transformOrigin: "280px 175px" }}
      >
        <circle cx="280" cy="180" r="20" fill="white" />
        {/* Bell body swings */}
        <motion.g
          animate={{ rotate: [-8, 8, -8] }}
          transition={{ duration: 0.6, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "280px 170px" }}
        >
          <path d="M272 183 C272 175 276 170 280 168 C284 170 288 175 288 183 L272 183 Z" fill="#FFB703" />
          <rect x="271" y="183" width="18" height="2.5" rx="1.25" fill="#FFB703" />
          <circle cx="280" cy="188" r="2.5" fill="#FFB703" />
        </motion.g>
        {/* Ring lines */}
        <motion.g
          animate={{ opacity: [0, 1, 0], scale: [0.8, 1.1, 0.8] }}
          transition={{ duration: 0.8, repeat: Infinity }}
          style={{ transformOrigin: "280px 168px" }}
        >
          <line x1="272" y1="168" x2="268" y2="164" stroke="#FFB703" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="288" y1="168" x2="292" y2="164" stroke="#FFB703" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="280" y1="164" x2="280" y2="160" stroke="#FFB703" strokeWidth="1.5" strokeLinecap="round" />
        </motion.g>
      </motion.g>

      {/* Heart floating up */}
      <motion.g
        animate={{ y: [0, -15, -30], opacity: [0, 1, 0], scale: [0.5, 1, 0.5] }}
        transition={{ duration: 2.5, repeat: Infinity, ease: "easeOut", delay: 1 }}
        style={{ transformOrigin: "100px 100px" }}
      >
        <path d="M95 105 C95 100 99 97 102 100 C105 97 109 100 109 105 C109 111 102 115 102 115 C102 115 95 111 95 105 Z" fill="#FF6B6B" opacity="0.6" />
      </motion.g>

      {/* Floating dots */}
      <motion.circle cx="60" cy="220" r="5" fill="#E0C5F5" opacity="0.6"
        animate={{ y: [-3, 5, -3], x: [-2, 2, -2] }}
        transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.circle cx="290" cy="250" r="4" fill="#B8E6C8" opacity="0.5"
        animate={{ y: [3, -4, 3] }}
        transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
      />
      <motion.circle cx="95" cy="90" r="3.5" fill="#FFB703" opacity="0.35"
        animate={{ y: [-2, 4, -2] }}
        transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut", delay: 0.8 }}
      />

      {/* Spinning diamond */}
      <motion.rect x="270" y="100" width="8" height="8" rx="2" fill="#FFDBA4" opacity="0.5"
        animate={{ rotate: [0, 180, 360], y: [-3, 3, -3] }}
        transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "274px 104px" }}
      />

      {/* Plus signs */}
      <motion.g opacity="0.25"
        animate={{ rotate: [0, 90, 0] }}
        transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "300px 136px" }}
      >
        <line x1="300" y1="130" x2="300" y2="142" stroke="#FFB703" strokeWidth="2" strokeLinecap="round" />
        <line x1="294" y1="136" x2="306" y2="136" stroke="#FFB703" strokeWidth="2" strokeLinecap="round" />
      </motion.g>
      <motion.g opacity="0.2"
        animate={{ rotate: [0, -90, 0] }}
        transition={{ duration: 7, repeat: Infinity, ease: "easeInOut" }}
        style={{ transformOrigin: "80px 285px" }}
      >
        <line x1="80" y1="280" x2="80" y2="290" stroke="#E0C5F5" strokeWidth="2" strokeLinecap="round" />
        <line x1="75" y1="285" x2="85" y2="285" stroke="#E0C5F5" strokeWidth="2" strokeLinecap="round" />
      </motion.g>
    </svg>
  );
}

export function ProtectedCircleIllustration() {
  return (
    <svg
      viewBox="0 0 360 360"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="w-full h-full"
    >
      {/* Background blob */}
      <ellipse cx="180" cy="180" rx="145" ry="140" fill="#F0E8FF" opacity="0.5" />

      {/* Central globe */}
      <circle cx="180" cy="175" r="65" fill="white" opacity="0.6" />
      <circle cx="180" cy="175" r="65" stroke="#E0C5F5" strokeWidth="1.5" fill="none" />
      <ellipse cx="180" cy="175" rx="65" ry="30" stroke="#E0C5F5" strokeWidth="1" fill="none" opacity="0.4" />
      <ellipse cx="180" cy="175" rx="30" ry="65" stroke="#E0C5F5" strokeWidth="1" fill="none" opacity="0.4" />
      <line x1="115" y1="175" x2="245" y2="175" stroke="#E0C5F5" strokeWidth="1" opacity="0.3" />
      <line x1="180" y1="110" x2="180" y2="240" stroke="#E0C5F5" strokeWidth="1" opacity="0.3" />

      {/* Orbit ring */}
      <motion.g
        animate={{ rotate: 360 }}
        transition={{ duration: 25, repeat: Infinity, ease: "linear" }}
        style={{ transformOrigin: "180px 175px" }}
      >
        <circle cx="180" cy="175" r="85" stroke="#D4C4F0" strokeWidth="1.5" strokeDasharray="8 5" fill="none" opacity="0.5" />
        <circle cx="265" cy="175" r="4" fill="#FFB703" />
      </motion.g>

      {/* === PERSON 1 - TOP: Woman (professional) - head tilt + blink === */}
      <motion.g
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3, duration: 0.6 }}
      >
        <circle cx="180" cy="68" r="35" fill="white" />
        <circle cx="180" cy="68" r="35" stroke="#E0C5F5" strokeWidth="1.5" fill="none" />

        {/* Body */}
        <path d="M167 78 C167 78 165 90 180 90 C195 90 193 78 193 78" fill="#E0C5F5" />

        {/* Head - gentle tilt */}
        <motion.g
          animate={{ rotate: [0, 3, 0, -3, 0] }}
          transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "180px 70px" }}
        >
          <circle cx="180" cy="60" r="14" fill="#FFDBA4" />

          {/* Hair */}
          <path d="M166 55 C165 42 172 35 180 35 C188 35 195 42 194 55 C192 48 188 42 180 42 C172 42 168 48 166 55 Z" fill="#2D2D2D" />
          <path d="M166 55 C164 62 165 68 167 68" fill="#2D2D2D" />
          <path d="M194 55 C196 62 195 68 193 68" fill="#2D2D2D" />

          {/* Eyes - blinking */}
          <motion.g
            animate={{ scaleY: [1, 1, 0.1, 1, 1, 1, 1, 0.1, 1, 1] }}
            transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut" }}
            style={{ transformOrigin: "180px 59px" }}
          >
            <circle cx="175" cy="59" r="1.8" fill="#2D2D2D" />
            <circle cx="185" cy="59" r="1.8" fill="#2D2D2D" />
          </motion.g>

          {/* Smile */}
          <path d="M176 65 C178 67.5 182 67.5 184 65" stroke="#2D2D2D" strokeWidth="1.5" strokeLinecap="round" fill="none" />
        </motion.g>

        {/* Phone icon pulse */}
        <motion.rect x="195" y="50" width="8" height="13" rx="2" fill="#FFB703"
          animate={{ opacity: [0.4, 0.8, 0.4] }}
          transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
        />
        <circle cx="199" cy="60" r="1" fill="white" />

        {/* Connection line */}
        <motion.line x1="180" y1="103" x2="180" y2="130"
          stroke="#E0C5F5" strokeWidth="2" strokeDasharray="4 3"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
          transition={{ delay: 1.2, duration: 0.4 }}
        />
      </motion.g>

      {/* === PERSON 2 - RIGHT: Doctor - adjusting glasses === */}
      <motion.g
        initial={{ opacity: 0, x: -15 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.5, duration: 0.6 }}
      >
        <circle cx="268" cy="145" r="32" fill="white" />
        <circle cx="268" cy="145" r="32" stroke="#B8E6C8" strokeWidth="1.5" fill="none" />

        <path d="M256 155 C256 155 254 166 268 166 C282 166 280 155 280 155" fill="white" stroke="#E5E5E5" strokeWidth="1" />
        <rect x="265.5" y="157" width="5" height="1.5" rx="0.75" fill="#FF6B6B" />
        <rect x="267.25" y="155.25" width="1.5" height="5" rx="0.75" fill="#FF6B6B" />

        {/* Head - slight nod */}
        <motion.g
          animate={{ y: [0, -1.5, 0, 1, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
        >
          <circle cx="268" cy="138" r="13" fill="#C68642" />

          {/* Hair */}
          <path d="M255 134 C255 126 261 121 268 121 C275 121 281 126 281 134 C279 130 275 127 268 127 C261 127 257 130 255 134 Z" fill="#1A1A1A" />

          {/* Glasses - slide up/down */}
          <motion.g
            animate={{ y: [0, -0.8, 0, 0.5, 0] }}
            transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
          >
            <circle cx="263" cy="137" r="4.5" stroke="#666" strokeWidth="1.2" fill="none" />
            <circle cx="273" cy="137" r="4.5" stroke="#666" strokeWidth="1.2" fill="none" />
            <line x1="267.5" y1="137" x2="268.5" y2="137" stroke="#666" strokeWidth="1" />
            <line x1="255" y1="136" x2="258.5" y2="137" stroke="#666" strokeWidth="1" />
            <line x1="281" y1="136" x2="277.5" y2="137" stroke="#666" strokeWidth="1" />
          </motion.g>

          {/* Eyes - blink */}
          <motion.g
            animate={{ scaleY: [1, 1, 1, 0.1, 1, 1, 1, 1, 1, 0.1, 1] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: "easeInOut" }}
            style={{ transformOrigin: "268px 137px" }}
          >
            <circle cx="263" cy="137" r="1.5" fill="#2D2D2D" />
            <circle cx="273" cy="137" r="1.5" fill="#2D2D2D" />
          </motion.g>

          <path d="M264 144 C266 146 270 146 272 144" stroke="#1A1A1A" strokeWidth="1.3" strokeLinecap="round" fill="none" />
        </motion.g>

        <motion.line x1="236" y1="155" x2="215" y2="168"
          stroke="#B8E6C8" strokeWidth="2" strokeDasharray="4 3"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
          transition={{ delay: 1.4, duration: 0.4 }}
        />
      </motion.g>

      {/* === PERSON 3 - BOTTOM RIGHT: Friend with headphones - head bob to music === */}
      <motion.g
        initial={{ opacity: 0, y: -15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7, duration: 0.6 }}
      >
        <circle cx="248" cy="268" r="32" fill="white" />
        <circle cx="248" cy="268" r="32" stroke="#FFB703" strokeWidth="1.5" fill="none" />

        <path d="M236 278 C236 278 234 289 248 289 C262 289 260 278 260 278" fill="#FF8C42" />

        {/* Head - bobbing to music */}
        <motion.g
          animate={{ rotate: [-5, 5, -5, 3, -5], y: [0, -2, 0, -1, 0] }}
          transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "248px 270px" }}
        >
          <circle cx="248" cy="260" r="13" fill="#FFDBA4" />

          {/* Curly hair */}
          <path d="M235 256 C234 246 240 240 248 240 C256 240 262 246 261 256 C259 250 255 246 248 246 C241 246 237 250 235 256 Z" fill="#D4854A" />
          <circle cx="237" cy="252" r="3" fill="#D4854A" />
          <circle cx="259" cy="252" r="3" fill="#D4854A" />
          <circle cx="248" cy="239" r="3" fill="#D4854A" />

          {/* Eyes - happy closed */}
          <path d="M243 259 C244 257 246 257 247 259" stroke="#2D2D2D" strokeWidth="1.8" strokeLinecap="round" fill="none" />
          <path d="M249 259 C250 257 252 257 253 259" stroke="#2D2D2D" strokeWidth="1.8" strokeLinecap="round" fill="none" />

          {/* Big smile - opens wider with bob */}
          <motion.path
            d="M243 265 C245 269 251 269 253 265"
            stroke="#2D2D2D" strokeWidth="1.5" strokeLinecap="round" fill="none"
            animate={{
              d: [
                "M243 265 C245 269 251 269 253 265",
                "M243 264 C245 270 251 270 253 264",
                "M243 265 C245 269 251 269 253 265",
              ],
            }}
            transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
          />

          {/* Headphones */}
          <path d="M235 255 C233 250 234 244 240 242" stroke="#555" strokeWidth="2.5" strokeLinecap="round" fill="none" />
          <path d="M261 255 C263 250 262 244 256 242" stroke="#555" strokeWidth="2.5" strokeLinecap="round" fill="none" />
          <circle cx="235" cy="257" r="3.5" fill="#555" />
          <circle cx="261" cy="257" r="3.5" fill="#555" />
        </motion.g>

        {/* Music notes floating */}
        <motion.text x="265" y="255" fontSize="10" fill="#FFB703" opacity="0.6"
          animate={{ y: [-5, -18], opacity: [0.7, 0], x: [0, 5] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeOut" }}
        >
          ♪
        </motion.text>
        <motion.text x="232" y="250" fontSize="8" fill="#E0C5F5" opacity="0.5"
          animate={{ y: [-3, -15], opacity: [0.6, 0], x: [0, -4] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeOut", delay: 0.6 }}
        >
          ♫
        </motion.text>

        <motion.line x1="222" y1="250" x2="205" y2="215"
          stroke="#FFB703" strokeWidth="2" strokeDasharray="4 3"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
          transition={{ delay: 1.6, duration: 0.4 }}
        />
      </motion.g>

      {/* === PERSON 4 - BOTTOM LEFT: Dad - gentle breathing + blink === */}
      <motion.g
        initial={{ opacity: 0, y: -15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.9, duration: 0.6 }}
      >
        <circle cx="112" cy="268" r="32" fill="white" />
        <circle cx="112" cy="268" r="32" stroke="#FFB703" strokeWidth="1.5" fill="none" />

        {/* Body - breathing */}
        <motion.path d="M100 278 C100 278 98 289 112 289 C126 289 124 278 124 278" fill="#3D5A80"
          animate={{ d: [
            "M100 278 C100 278 98 289 112 289 C126 289 124 278 124 278",
            "M99 278 C99 278 97 290 112 290 C127 290 125 278 125 278",
            "M100 278 C100 278 98 289 112 289 C126 289 124 278 124 278",
          ]}}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        />

        {/* Head - slight movement */}
        <motion.g
          animate={{ rotate: [0, 2, 0, -1, 0] }}
          transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "112px 268px" }}
        >
          <circle cx="112" cy="260" r="13" fill="#FFDBA4" />

          <path d="M99 256 C99 247 105 241 112 241 C119 241 125 247 125 256 C123 251 119 247 112 247 C105 247 101 251 99 256 Z" fill="#888" />

          {/* Eyes - slow blink */}
          <motion.g
            animate={{ scaleY: [1, 1, 1, 1, 0.1, 1, 1, 1, 1, 1] }}
            transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
            style={{ transformOrigin: "112px 259px" }}
          >
            <circle cx="108" cy="259" r="1.8" fill="#2D2D2D" />
            <circle cx="116" cy="259" r="1.8" fill="#2D2D2D" />
          </motion.g>

          {/* Kind wrinkles */}
          <path d="M105 256 C106 254 109 254 110 256" stroke="#E8B888" strokeWidth="1" strokeLinecap="round" fill="none" opacity="0.5" />
          <path d="M114 256 C115 254 118 254 119 256" stroke="#E8B888" strokeWidth="1" strokeLinecap="round" fill="none" opacity="0.5" />

          {/* Smile - warm */}
          <motion.path
            d="M108 265 C110 267.5 114 267.5 116 265"
            stroke="#2D2D2D" strokeWidth="1.3" strokeLinecap="round" fill="none"
            animate={{
              d: [
                "M108 265 C110 267.5 114 267.5 116 265",
                "M108 265 C110 268 114 268 116 265",
                "M108 265 C110 267.5 114 267.5 116 265",
              ],
            }}
            transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          />
        </motion.g>

        {/* Waving hand */}
        <motion.g
          animate={{ rotate: [0, 15, -5, 15, 0] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut", repeatDelay: 2 }}
          style={{ transformOrigin: "130px 278px" }}
        >
          <path d="M126 278 C130 275 133 272 134 268" stroke="#FFDBA4" strokeWidth="4" strokeLinecap="round" fill="none" />
          <circle cx="134" cy="267" r="3" fill="#FFDBA4" />
          {/* Fingers */}
          <line x1="133" y1="264" x2="131" y2="261" stroke="#FFDBA4" strokeWidth="2" strokeLinecap="round" />
          <line x1="135" y1="264" x2="135" y2="260" stroke="#FFDBA4" strokeWidth="2" strokeLinecap="round" />
          <line x1="137" y1="265" x2="138" y2="261" stroke="#FFDBA4" strokeWidth="2" strokeLinecap="round" />
        </motion.g>

        <motion.line x1="138" y1="250" x2="155" y2="215"
          stroke="#FFB703" strokeWidth="2" strokeDasharray="4 3"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
          transition={{ delay: 1.8, duration: 0.4 }}
        />
      </motion.g>

      {/* === PERSON 5 - LEFT: Mom/Sister - hair sway + blink === */}
      <motion.g
        initial={{ opacity: 0, x: 15 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 1.1, duration: 0.6 }}
      >
        <circle cx="92" cy="145" r="32" fill="white" />
        <circle cx="92" cy="145" r="32" stroke="#E0C5F5" strokeWidth="1.5" fill="none" />

        <path d="M80 155 C80 155 78 166 92 166 C106 166 104 155 104 155" fill="#E0C5F5" />

        {/* Head - gentle sway */}
        <motion.g
          animate={{ rotate: [0, -3, 0, 3, 0] }}
          transition={{ duration: 4.5, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "92px 150px" }}
        >
          <circle cx="92" cy="138" r="13" fill="#8D5524" />

          {/* Hair top */}
          <path d="M79 134 C78 124 84 118 92 118 C100 118 106 124 105 134 C103 128 99 124 92 124 C85 124 81 128 79 134 Z" fill="#1A1A1A" />

          {/* Long hair - swaying */}
          <motion.path d="M79 134 C77 142 77 152 80 158" stroke="#1A1A1A" strokeWidth="4" strokeLinecap="round" fill="none"
            animate={{ d: [
              "M79 134 C77 142 77 152 80 158",
              "M79 134 C76 142 75 152 78 158",
              "M79 134 C77 142 77 152 80 158",
            ]}}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.path d="M105 134 C107 142 107 152 104 158" stroke="#1A1A1A" strokeWidth="4" strokeLinecap="round" fill="none"
            animate={{ d: [
              "M105 134 C107 142 107 152 104 158",
              "M105 134 C108 142 109 152 106 158",
              "M105 134 C107 142 107 152 104 158",
            ]}}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />

          {/* Eyes - blink */}
          <motion.g
            animate={{ scaleY: [1, 1, 0.1, 1, 1, 1, 1, 1, 0.1, 1] }}
            transition={{ duration: 3.8, repeat: Infinity, ease: "easeInOut", delay: 0.3 }}
            style={{ transformOrigin: "92px 137px" }}
          >
            <circle cx="88" cy="137" r="1.8" fill="#2D2D2D" />
            <circle cx="96" cy="137" r="1.8" fill="#2D2D2D" />
          </motion.g>

          <path d="M88 143 C90 145.5 94 145.5 96 143" stroke="#1A1A1A" strokeWidth="1.3" strokeLinecap="round" fill="none" />

          {/* Blush */}
          <motion.ellipse cx="84" cy="141" rx="3.5" ry="2" fill="#D4854A"
            animate={{ opacity: [0.2, 0.4, 0.2] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.ellipse cx="100" cy="141" rx="3.5" ry="2" fill="#D4854A"
            animate={{ opacity: [0.2, 0.4, 0.2] }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
          />
        </motion.g>

        {/* Heart - beating */}
        <motion.path d="M74 128 C74 125 77 123 79 125 C81 123 84 125 84 128 C84 132 79 135 79 135 C79 135 74 132 74 128 Z" fill="#FF6B6B"
          animate={{ scale: [1, 1.15, 1, 1.15, 1] }}
          transition={{ duration: 1.2, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "79px 130px" }}
        />

        <motion.line x1="120" y1="155" x2="145" y2="168"
          stroke="#E0C5F5" strokeWidth="2" strokeDasharray="4 3"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
          transition={{ delay: 2, duration: 0.4 }}
        />
      </motion.g>

      {/* === CENTRAL SHIELD - pulse === */}
      <motion.g
        initial={{ scale: 0.5, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ delay: 0.15, duration: 0.5, ease: [0.175, 0.885, 0.32, 1.275] }}
      >
        <motion.g
          animate={{ scale: [1, 1.04, 1] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "180px 178px" }}
        >
          <path d="M162 160 L180 148 L198 160 L198 188 C198 198 180 208 180 208 C180 208 162 198 162 188 Z" fill="url(#shieldGradient2)" />
          <path d="M172 178 L178 184 L190 170" stroke="white" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        </motion.g>
      </motion.g>

      {/* Pulse rings */}
      <motion.circle cx="180" cy="178" r="30" stroke="#FFB703" strokeWidth="1.5" fill="none"
        animate={{ opacity: [0.4, 0], scale: [1, 1.8] }}
        transition={{ duration: 2, repeat: Infinity, ease: "easeOut" }}
      />
      <motion.circle cx="180" cy="178" r="30" stroke="#FFB703" strokeWidth="1" fill="none"
        animate={{ opacity: [0.3, 0], scale: [1, 1.8] }}
        transition={{ duration: 2, repeat: Infinity, ease: "easeOut", delay: 0.7 }}
      />

      {/* Floating sparkle stars */}
      <motion.g animate={{ y: [-3, 3, -3], rotate: [0, 15, 0] }} transition={{ duration: 3.5, repeat: Infinity, ease: "easeInOut" }}>
        <g transform="translate(305, 80)" opacity="0.35">
          <path d="M0 -6 L1.5 -1.5 L6 0 L1.5 1.5 L0 6 L-1.5 1.5 L-6 0 L-1.5 -1.5 Z" fill="#FFB703" />
        </g>
      </motion.g>
      <motion.g animate={{ y: [3, -4, 3], rotate: [0, -20, 0] }} transition={{ duration: 4, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}>
        <g transform="translate(50, 310)" opacity="0.3">
          <path d="M0 -5 L1.2 -1.2 L5 0 L1.2 1.2 L0 5 L-1.2 1.2 L-5 0 L-1.2 -1.2 Z" fill="#E0C5F5" />
        </g>
      </motion.g>
      <motion.g animate={{ y: [-2, 4, -2] }} transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 1 }}>
        <g transform="translate(310, 300)" opacity="0.25">
          <path d="M0 -4 L1 -1 L4 0 L1 1 L0 4 L-1 1 L-4 0 L-1 -1 Z" fill="#B8E6C8" />
        </g>
      </motion.g>

      <defs>
        <linearGradient id="shieldGradient2" x1="162" y1="148" x2="198" y2="208" gradientUnits="userSpaceOnUse">
          <stop stopColor="#FFD54F" />
          <stop offset="1" stopColor="#FFB703" />
        </linearGradient>
      </defs>
    </svg>
  );
}
