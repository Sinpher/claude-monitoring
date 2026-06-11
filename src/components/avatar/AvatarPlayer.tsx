import { AgentStatusType, AvatarPack } from "../../types";

/** 内置形象包配置 */
const BUILT_IN_PACKS: Record<string, AvatarPack> = {
  panda: { name: "panda", label: "熊猫", animations: { WORKING: "", DONE: "", WAITING: "", IDLE: "" } },
  cat: { name: "cat", label: "猫咪", animations: { WORKING: "", DONE: "", WAITING: "", IDLE: "" } },
  robot: { name: "robot", label: "机器人", animations: { WORKING: "", DONE: "", WAITING: "", IDLE: "" } },
};

/** 形象包主色 */
const PACK_COLORS: Record<string, Record<AgentStatusType, string>> = {
  panda: { WORKING: "#4CAF50", DONE: "#2196F3", WAITING: "#FF9800", IDLE: "#9E9E9E" },
  cat: { WORKING: "#AB47BC", DONE: "#26C6DA", WAITING: "#FFA726", IDLE: "#78909C" },
  robot: { WORKING: "#00BCD4", DONE: "#66BB6A", WAITING: "#FF7043", IDLE: "#607D8B" },
};

/** 形象包内部图案（emoji） */
const PACK_FACES: Record<string, string> = {
  panda: "🐼",
  cat: "🐱",
  robot: "🤖",
};

/** 获取所有可用形象包列表 */
export function getAvailablePacks(): AvatarPack[] {
  return Object.values(BUILT_IN_PACKS);
}

interface AvatarPlayerProps {
  /** 当前 Agent 状态 */
  status: AgentStatusType;
  /** 形象包名称，默认 panda */
  packName?: string;
  /** 动画尺寸（像素） */
  size?: number;
}

/**
 * 纯 SVG+CSS 动画形象组件，根据 Agent 状态展示不同动画效果。
 * 不依赖 Lottie，在透明窗口中也能正常渲染。
 */
export function AvatarPlayer({
  status,
  packName = "panda",
  size = 64,
}: AvatarPlayerProps) {
  const colors = PACK_COLORS[packName] || PACK_COLORS.panda;
  const color = colors[status];
  const face = PACK_FACES[packName] || PACK_FACES.panda;
  const fontSize = Math.round(size * 0.5);

  // 根据状态选择动画类名
  const animClass =
    status === "WORKING" ? "avatar-pulse" :
    status === "DONE" ? "avatar-bounce" :
    status === "WAITING" ? "avatar-shake" :
    "avatar-breathe";

  return (
    <div
      className={`flex items-center justify-center ${animClass}`}
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} viewBox="0 0 64 64">
        {/* 外圈光晕 */}
        <circle cx="32" cy="32" r="30" fill={color} opacity="0.15" />
        {/* 主圆 */}
        <circle cx="32" cy="32" r="24" fill={color} opacity="0.3" />
        <circle cx="32" cy="32" r="20" fill={color} opacity="0.9" />
        {/* 内部图案用emoji覆盖 */}
        <text
          x="32"
          y="36"
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize > 20 ? 20 : fontSize}
        >
          {face}
        </text>
      </svg>
    </div>
  );
}
