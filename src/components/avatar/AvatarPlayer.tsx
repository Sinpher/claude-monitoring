import { useEffect, useRef } from "react";
import lottie, { AnimationItem } from "lottie-web";
import { AgentStatusType, AvatarPack } from "../../types";

/** 内置形象包注册表 */
const BUILT_IN_PACKS: Record<string, AvatarPack> = {
  panda: {
    name: "panda",
    label: "熊猫",
    animations: {
      WORKING: "/avatars/panda/working.json",
      DONE: "/avatars/panda/done.json",
      WAITING: "/avatars/panda/waiting.json",
      IDLE: "/avatars/panda/idle.json",
    },
  },
  cat: {
    name: "cat",
    label: "猫咪",
    animations: {
      WORKING: "/avatars/cat/working.json",
      DONE: "/avatars/cat/done.json",
      WAITING: "/avatars/cat/waiting.json",
      IDLE: "/avatars/cat/idle.json",
    },
  },
  robot: {
    name: "robot",
    label: "机器人",
    animations: {
      WORKING: "/avatars/robot/working.json",
      DONE: "/avatars/robot/done.json",
      WAITING: "/avatars/robot/waiting.json",
      IDLE: "/avatars/robot/idle.json",
    },
  },
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
 * Lottie 动画播放器组件，根据 Agent 状态播放对应动画。
 */
export function AvatarPlayer({
  status,
  packName = "panda",
  size = 64,
}: AvatarPlayerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<AnimationItem | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // 销毁旧动画
    animationRef.current?.destroy();

    const pack = BUILT_IN_PACKS[packName] || BUILT_IN_PACKS.panda;
    const animationPath = pack.animations[status];

    // 加载并播放新动画
    animationRef.current = lottie.loadAnimation({
      container: containerRef.current,
      renderer: "svg",
      loop: status !== "DONE",
      autoplay: true,
      path: animationPath,
    });

    return () => {
      animationRef.current?.destroy();
    };
  }, [status, packName]);

  return (
    <div
      ref={containerRef}
      style={{ width: size, height: size }}
      className="flex items-center justify-center"
    />
  );
}
