# MMCE Parallel Equalizer / MMCE 并行均分仓

Minecraft 1.12.2 的 Modular Machinery: Community Edition（MMCE）附属模组。  
An add-on for Modular Machinery: Community Edition (MMCE) on Minecraft 1.12.2.

| 项目 / Item | 信息 / Information |
| --- | --- |
| 模组 ID / Mod ID | `mmceparallelequalizer` |
| 当前版本 / Current version | `1.0.0` |
| 作者 / Author | GingerYJ |
| Minecraft | `1.12.2` |
| MMCE | `2.3.2` |

## 简介 / Overview

**中文：** 本模组添加“并行均分仓”。当它作为 MMCE 工厂结构的一部分时，会将机器当前的有效并行数均分给全部工厂线程，防止单个线程占用全部并行。

**English:** This mod adds the Parallel Equalizer Hatch. When included in an MMCE factory structure, it divides the machine's current effective parallelism among all factory threads so that one thread cannot take the entire parallelism budget.

## 功能 / Features

- **线程均分 / Per-thread allocation:** 每个配方线程在启动时获得固定的并行上限。 / Each recipe thread receives a fixed parallelism limit when it starts.
- **轻量运行 / Lightweight operation:** 均分仓不会创建额外线程，也不会为配方增加材料或能源需求。 / The hatch creates no extra threads and adds no material or energy requirements to recipes.
- **专属创造栏 / Creative tab:** 方块位于独立的 MMCE Parallel Equalizer 创造栏中。 / The block is available in its own MMCE Parallel Equalizer creative tab.

## 环境与安装 / Requirements and Installation

| 依赖 / Dependency | 要求 / Requirement |
| --- | --- |
| Minecraft | `1.12.2` |
| 加载器 / Loader | Cleanroom Loader（已在 `0.5.12-alpha` 测试 / tested with `0.5.12-alpha`） |
| Java | `21` 或更高 / `21` or newer |
| Modular Machinery: Community Edition | `>= 2.3.2` |

将 MMCE、本模组及 MMCE 自身所需依赖同时放入客户端与服务端的 `mods` 文件夹。此模组不会把 MMCE 或其依赖打包进自身 JAR。  
Place MMCE, this mod, and MMCE's own runtime dependencies in the `mods` folder on the client and server. MMCE and its dependencies are not bundled inside this mod's JAR.

## 机器 JSON / Machine JSON

均分仓必须作为机器结构 JSON 的直接组件写入；仅在机器旁边放置方块或写入变量组都不会绑定。下面的坐标只是示例，请按实际结构调整。
The hatch must be declared directly in the machine structure JSON. Placing it next to a machine or adding it through a variable group does not bind it. The coordinates below are only an example and must be adjusted for the actual structure.

```json
{
  "parts": [
    {
      "x": 0,
      "y": 0,
      "z": 1,
      "elements": "mmceparallelequalizer:parallel_equalizer_hatch@0"
    }
  ]
}
```

均分逻辑只作用于 MMCE 工厂控制器，因此机器需要启用工厂模式，例如：  
The equalization logic only applies to MMCE factory controllers, so the machine must use factory mode, for example:

```json
{
  "has-factory": true,
  "factory-only": true
}
```

## CraftTweaker 示例 / CraftTweaker Example

以下脚本创建一个拥有 `4` 个线程和 `64` 有效并行的测试工厂。配方不需要能源输入。  
The following script configures a test factory with `4` threads and `64` effective parallelism. The recipe has no energy input.

```zenscript
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.RecipeBuilder;

MachineModifier.setMaxThreads("parallel_equalizer_test", 4);
MachineModifier.setMaxParallelism("parallel_equalizer_test", 64);
MachineModifier.setInternalParallelism("parallel_equalizer_test", 64);

RecipeBuilder.newBuilder("parallel_equalizer_test_recipe", "parallel_equalizer_test", 100)
    .addItemInput(<minecraft:cobblestone>)
    .addItemOutput(<minecraft:stone>)
    .setParallelized(true)
    .build();
```

`setMaxParallelism` 只设置并行上限，不会提供并行。机器的实际有效并行必须来自 `setInternalParallelism` 或结构中的 MMCE 并行控制器；均分仓本身不会提高总并行。配方必须启用 `.setParallelized(true)` 才能使用并行。  
`setMaxParallelism` only sets the upper limit and does not provide parallelism. Effective parallelism must come from `setInternalParallelism` or MMCE Parallel Controllers in the structure; the hatch does not increase total parallelism. Recipes must enable `.setParallelized(true)` to use parallelism.

## 分配规则 / Allocation Rules

单线程均分值按以下方式计算：  
The per-thread allocation is calculated as follows:

```text
单线程并行 = max(1, floor(机器有效并行 / 线程槽数量))
Per-thread parallelism = max(1, floor(effective machine parallelism / thread slots))
```

线程槽数量包含普通工厂线程和已配置的核心线程。配方最终使用的并行数不会超过配方自身可用并行或单线程均分值。  
The thread count includes regular factory threads and configured core threads. A recipe's final parallelism cannot exceed either its own available parallelism or the per-thread allocation.

示例：`64 / 4 = 16`，所以每个线程最多使用 `16` 并行。  
Example: `64 / 4 = 16`, so each thread can use at most `16` parallelism.

注意事项 / Notes:

- 计算使用整数除法，不能整除的余数不会分配。 / Integer division is used, so any remainder is left unused.
- 空闲线程的份额不会动态转移给正在工作的线程。 / An idle thread's share is not dynamically reassigned to active threads.
- 均分仓限制的是配方启动时的并行数，不会修改 MMCE 配置的最大线程数或最大并行数。 / The hatch limits parallelism when a recipe starts; it does not modify MMCE's configured maximum threads or maximum parallelism.
- 同一台机器放置多个均分仓不会叠加均分效果。 / Multiple hatches in the same machine do not stack their equalization effect.

## 合成 / Crafting

```text
I R I
R P R
I R I
```

- `I`: 铁锭 / Iron Ingot
- `R`: 红石 / Redstone
- `P`: MMCE 并行控制器 / MMCE Parallel Controller

## 构建 / Building

开发环境默认从项目上级目录读取 `ModularMachinery-CE-2.3.2.jar`，使用 Java 25 工具链并生成 Java 21 目标字节码。Gradle 可通过已配置的 Foojay 解析器自动获取工具链。  
The development environment expects `ModularMachinery-CE-2.3.2.jar` in the parent directory by default. It uses a Java 25 toolchain and produces Java 21 target bytecode. Gradle can provision the toolchain through the configured Foojay resolver.

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs/mmceparallelequalizer-1.0.0.jar`。  
The built mod is written to `build/libs/mmceparallelequalizer-1.0.0.jar`.

## 许可证 / License

本项目采用 [MIT License](LICENSE)。  
This project is licensed under the [MIT License](LICENSE).
