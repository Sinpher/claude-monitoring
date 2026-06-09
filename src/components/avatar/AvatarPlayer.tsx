import { useEffect, useRef } from "react";
import lottie, { AnimationItem } from "lottie-web";
import { AgentStatusType, AvatarPack } from "../../types";

/** 内置熊猫形象包 */
const PANDA_PACK: AvatarPack = {
  name: "panda",
  label: "熊猫",
  animations: {
    WORKING: "/avatars/panda/working.json",
    DONE: "/avatars/panda/done.json",
    WAITING: "/avatars/panda/waiting.json",
    IDLE: "/avatars/panda/idle.json",
  },
};

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

    const pack = packName === "panda" ? PANDA_PACK : PANDA_PACK;
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
