package com.xbodw.blink.block;

import com.xbodw.blink.Blink;
import com.xbodw.blink.dimension.BlinkDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
//import net.minecraft.world.level.levelgen.structure.structures.VillageStructures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlinkPortalBlock extends Block {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    
    // 使用静态Map来存储玩家数据，避免NBT问题
    private static final Map<UUID, Long> lastTeleportTime = new HashMap<>();
    private static final Map<UUID, BlockPos> playerReturnPositions = new HashMap<>();
    private static boolean villageGenerated = false; // 标记村庄是否已生成
    
    public BlinkPortalBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            // 添加冷却时间，防止重复传送
            UUID playerId = player.getUUID();
            long currentTime = level.getGameTime();
            Long lastTeleport = lastTeleportTime.get(playerId);
            
            if (lastTeleport != null && currentTime - lastTeleport < 40) { // 2秒冷却，增加冷却时间
                return;
            }
            
            lastTeleportTime.put(playerId, currentTime);
            
            // 添加调试信息
            player.sendSystemMessage(Component.literal("传送门触发！当前维度: " + level.dimension().location()));
            
            if (level.dimension() == BlinkDimensions.BLINK_DIMENSION) {
                // 从Blink维度返回主世界
                player.sendSystemMessage(Component.literal("正在返回主世界..."));
                teleportToOverworld(player, pos);
            } else {
                // 从主世界进入Blink维度
                player.sendSystemMessage(Component.literal("正在进入Blink维度..."));
                teleportToBlinkDimension(player, pos);
            }
        }
    }
    
    private void teleportToBlinkDimension(ServerPlayer player, BlockPos portalPos) {
        try {
            if (player.getServer() != null) {
                ServerLevel blinkLevel = player.getServer().getLevel(BlinkDimensions.BLINK_DIMENSION);
                if (blinkLevel != null) {
                    player.sendSystemMessage(Component.literal("找到Blink维度，开始传送..."));
                    
                    // 保存玩家在主世界的传送门位置
                    playerReturnPositions.put(player.getUUID(), portalPos);
                    
                    // 传送到Blink维度的spawn点
                    BlockPos targetPos = new BlockPos(0, 120, 0);
                    
                    // 确保目标位置安全并生成返回传送门和村庄
                    ensureSafeSpawnWithPortalAndVillage(blinkLevel, targetPos);
                    
                    // 在传送门附近找一个安全位置传送
                    BlockPos safePos = findSafePositionInBlinkDimension(blinkLevel, targetPos);
                    
                    player.sendSystemMessage(Component.literal("传送到位置: " + safePos));
                    
                    // 播放传送音效
                    player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    // 使用teleportTo方法直接传送
                    player.teleportTo(blinkLevel, 
                        safePos.getX() + 0.5, 
                        safePos.getY(), 
                        safePos.getZ() + 0.5, 
                        player.getYRot(), 
                        player.getXRot());
                    
                    player.sendSystemMessage(Component.literal("传送成功！"));
                    // 传送后再次播放音效
                    player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    player.sendSystemMessage(Component.literal("错误：无法找到Blink维度！"));
                }
            } else {
                player.sendSystemMessage(Component.literal("错误：服务器为空！"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("传送错误: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private void teleportToOverworld(ServerPlayer player, BlockPos portalPos) {
        try {
            if (player.getServer() != null) {
                ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    player.sendSystemMessage(Component.literal("找到主世界，开始传送..."));
                    
                    // 获取玩家保存的返回位置
                    BlockPos returnPos = playerReturnPositions.get(player.getUUID());
                    
                    if (returnPos == null) {
                        // 如果没有保存位置，使用世界出生点
                        returnPos = overworld.getSharedSpawnPos();
                        player.sendSystemMessage(Component.literal("使用世界出生点: " + returnPos));
                    } else {
                        player.sendSystemMessage(Component.literal("使用保存的返回位置: " + returnPos));
                    }
                    
                    // 在传送门旁找一个安全位置（主世界不创建方块）
                    BlockPos safePos = findSafePositionNearPortal(overworld, returnPos, false);
                    
                    player.sendSystemMessage(Component.literal("传送到位置: " + safePos));
                    
                    // 播放传送音效
                    player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    // 使用teleportTo方法直接传送
                    player.teleportTo(overworld, 
                        safePos.getX() + 0.5, 
                        safePos.getY(), 
                        safePos.getZ() + 0.5, 
                        player.getYRot(), 
                        player.getXRot());
                    
                    player.sendSystemMessage(Component.literal("返回主世界成功！"));
                    // 传送后再次播放音效
                    player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    player.sendSystemMessage(Component.literal("错误：无法找到主世界！"));
                }
            } else {
                player.sendSystemMessage(Component.literal("错误：服务器为空！"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("返回错误: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private BlockPos findSafePositionNearPortal(ServerLevel level, BlockPos portalPos, boolean canCreateBlocks) {
        // 在传送门周围寻找安全位置
        BlockPos[] offsets = {
            new BlockPos(2, 0, 0),   // 东边
            new BlockPos(-2, 0, 0),  // 西边
            new BlockPos(0, 0, 2),   // 南边
            new BlockPos(0, 0, -2),  // 北边
            new BlockPos(1, 0, 1),   // 东南
            new BlockPos(-1, 0, 1),  // 西南
            new BlockPos(1, 0, -1),  // 东北
            new BlockPos(-1, 0, -1)  // 西北
        };
        
        for (BlockPos offset : offsets) {
            BlockPos testPos = portalPos.offset(offset);
            if (isSafePosition(level, testPos)) {
                return testPos;
            }
        }
        
        // 如果找不到安全位置且允许创建方块，创建一个
        if (canCreateBlocks) {
            BlockPos safePos = portalPos.offset(2, 0, 0);
            level.setBlock(safePos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(safePos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(safePos.above(), Blocks.AIR.defaultBlockState(), 3);
            return safePos;
        }
        
        // 主世界中如果找不到安全位置，返回传送门位置上方
        return portalPos.above();
    }
    
    private BlockPos findSafePositionInBlinkDimension(ServerLevel level, BlockPos centerPos) {
        // 首先尝试在传送门平台上找位置
        BlockPos[] nearbyOffsets = {
            new BlockPos(2, 0, 0),   // 东边
            new BlockPos(-2, 0, 0),  // 西边
            new BlockPos(0, 0, 2),   // 南边
            new BlockPos(0, 0, -2),  // 北边
            new BlockPos(0, 1, 0)    // 上方
        };
        
        for (BlockPos offset : nearbyOffsets) {
            BlockPos testPos = centerPos.offset(offset);
            if (isSafePosition(level, testPos)) {
                return testPos;
            }
        }
        
        // 如果平台上没有安全位置，在村庄附近寻找
        BlockPos[] villageOffsets = {
            new BlockPos(50, 0, 50),   // 村庄中心附近
            new BlockPos(-50, 0, 50),
            new BlockPos(50, 0, -50),
            new BlockPos(-50, 0, -50),
            new BlockPos(25, 0, 25),
            new BlockPos(-25, 0, 25),
            new BlockPos(25, 0, -25),
            new BlockPos(-25, 0, -25)
        };
        
        for (BlockPos offset : villageOffsets) {
            BlockPos testPos = centerPos.offset(offset);
            // 找到地面高度
            for (int y = 150; y > 60; y--) {
                BlockPos groundPos = new BlockPos(testPos.getX(), y, testPos.getZ());
                if (!level.getBlockState(groundPos).isAir() && 
                    level.getBlockState(groundPos.above()).isAir() && 
                    level.getBlockState(groundPos.above(2)).isAir()) {
                    return groundPos.above();
                }
            }
        }
        
        // 如果都找不到，强制创建一个安全位置
        BlockPos safePos = centerPos.offset(3, 0, 0);
        level.setBlock(safePos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(safePos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(safePos.above(), Blocks.AIR.defaultBlockState(), 3);
        return safePos;
    }
    
    private boolean isSafePosition(ServerLevel level, BlockPos pos) {
        // 检查脚下有实体方块，头部和身体位置是空气
        return !level.getBlockState(pos.below()).isAir() && 
               level.getBlockState(pos).isAir() && 
               level.getBlockState(pos.above()).isAir();
    }
    
    private void ensureSafeSpawnWithPortalAndVillage(ServerLevel level, BlockPos pos) {
        // 创建一个大平台
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                BlockPos platformPos = pos.offset(x, -1, z);
                level.setBlock(platformPos, Blocks.STONE.defaultBlockState(), 3);
            }
        }
        
        // 清理spawn区域
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 5; y++) {
                    BlockPos clearPos = pos.offset(x, y, z);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        
        // 在Blink维度创建返回传送门框架和传送门
        if (level.dimension() == BlinkDimensions.BLINK_DIMENSION) {
            BlockPos portalCenter = pos.offset(8, 0, 0);
            
            // 创建强化深板岩框架 (4x5)
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 5; y++) {
                    BlockPos framePos = portalCenter.offset(x - 2, y, 0);
                    boolean isFrame = (x == 0 || x == 3) || (y == 0 || y == 4);
                    
                    if (isFrame) {
                        level.setBlock(framePos, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(framePos, Blink.BLINK_PORTAL_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
            
            // 在框架周围放置一些装饰和照明
            level.setBlock(portalCenter.offset(-3, 0, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
            level.setBlock(portalCenter.offset(2, 0, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
            level.setBlock(portalCenter.offset(-1, 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
            level.setBlock(portalCenter.offset(0, 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
            
            // 确保传送门旁边有安全的着陆点
            BlockPos landingPos = portalCenter.offset(2, 0, 0);
            level.setBlock(landingPos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(landingPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(landingPos.above(), Blocks.AIR.defaultBlockState(), 3);
            
            // 生成大型村庄（只生成一次）
            if (!villageGenerated) {
                generateMegaVillage(level, pos);
                villageGenerated = true;
            }
        }
    }
    
    private void generateMegaVillage(ServerLevel level, BlockPos centerPos) {
        // 创建一个由10个小村庄组成的大型村庄
        BlockPos[] villagePositions = {
            centerPos.offset(100, 0, 100),   // 东北
            centerPos.offset(-100, 0, 100),  // 西北
            centerPos.offset(100, 0, -100),  // 东南
            centerPos.offset(-100, 0, -100), // 西南
            centerPos.offset(150, 0, 0),     // 东
            centerPos.offset(-150, 0, 0),    // 西
            centerPos.offset(0, 0, 150),     // 北
            centerPos.offset(0, 0, -150),    // 南
            centerPos.offset(75, 0, 75),     // 中心东北
            centerPos.offset(-75, 0, -75)    // 中心西南
        };
        
        for (BlockPos villagePos : villagePositions) {
            generateSingleVillage(level, villagePos);
        }
        
        // 在村庄之间创建道路连接
        createVillageRoads(level, centerPos, villagePositions);
    }
    
    private void generateSingleVillage(ServerLevel level, BlockPos villageCenter) {
        // 为每个村庄创建平台
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                BlockPos groundPos = villageCenter.offset(x, -1, z);
                level.setBlock(groundPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }
        
        // 生成村庄建筑
        generateVillageBuildings(level, villageCenter);
        
        // 添加装饰和照明
        addVillageDecorations(level, villageCenter);
    }
    
    private void generateVillageBuildings(ServerLevel level, BlockPos center) {
        // 生成各种村庄建筑
        BlockPos[] buildingPositions = {
            center.offset(0, 0, 0),      // 中心建筑
            center.offset(15, 0, 15),    // 房屋1
            center.offset(-15, 0, 15),   // 房屋2
            center.offset(15, 0, -15),   // 房屋3
            center.offset(-15, 0, -15),  // 房屋4
            center.offset(25, 0, 0),     // 农场
            center.offset(-25, 0, 0),    // 铁匠铺
            center.offset(0, 0, 25),     // 图书馆
            center.offset(0, 0, -25)     // 教堂
        };
        
        String[] buildingTypes = {
            "town_center", "house", "house", "house", "house", 
            "farm", "blacksmith", "library", "church"
        };
        
        for (int i = 0; i < buildingPositions.length; i++) {
            generateBuilding(level, buildingPositions[i], buildingTypes[i]);
        }
    }
    
    private void generateBuilding(ServerLevel level, BlockPos pos, String type) {
        switch (type) {
            case "town_center":
                generateTownCenter(level, pos);
                break;
            case "house":
                generateHouse(level, pos);
                break;
            case "farm":
                generateFarm(level, pos);
                break;
            case "blacksmith":
                generateBlacksmith(level, pos);
                break;
            case "library":
                generateLibrary(level, pos);
                break;
            case "church":
                generateChurch(level, pos);
                break;
        }
    }
    
    private void generateTownCenter(ServerLevel level, BlockPos center) {
        // 生成一个大型中心建筑
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 8; y++) {
                    BlockPos buildPos = center.offset(x, y, z);
                    
                    if (y == 0) {
                        level.setBlock(buildPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    } else if (y == 8 || Math.abs(x) == 5 || Math.abs(z) == 5) {
                        if (y < 6) {
                            level.setBlock(buildPos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        } else {
                            level.setBlock(buildPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                        }
                    } else {
                        level.setBlock(buildPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // 添加入口
        level.setBlock(center.offset(0, 1, 5), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 2, 5), Blocks.AIR.defaultBlockState(), 3);
        
        // 添加照明
        level.setBlock(center.offset(0, 6, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
    }
    
    private void generateHouse(ServerLevel level, BlockPos center) {
        // 生成一个简单的房屋
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 4; y++) {
                    BlockPos buildPos = center.offset(x, y, z);
                    
                    if (y == 0) {
                        level.setBlock(buildPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    } else if (y == 4 || Math.abs(x) == 3 || Math.abs(z) == 3) {
                        if (y == 4) {
                            level.setBlock(buildPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                        } else {
                            level.setBlock(buildPos, Blocks.OAK_LOG.defaultBlockState(), 3);
                        }
                    } else {
                        level.setBlock(buildPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // 添加门
        level.setBlock(center.offset(0, 1, 3), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 2, 3), Blocks.AIR.defaultBlockState(), 3);
        
        // 添加窗户
        level.setBlock(center.offset(2, 2, 0), Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(-2, 2, 0), Blocks.GLASS.defaultBlockState(), 3);
    }
    
    private void generateFarm(ServerLevel level, BlockPos center) {
        // 生成农场
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos farmPos = center.offset(x, 0, z);
                if (Math.abs(x) <= 6 && Math.abs(z) <= 6) {
                    level.setBlock(farmPos, Blocks.FARMLAND.defaultBlockState(), 3);
                    if ((x + z) % 2 == 0) {
                        level.setBlock(farmPos.above(), Blocks.WHEAT.defaultBlockState(), 3);
                    }
                } else {
                    level.setBlock(farmPos, Blocks.DIRT_PATH.defaultBlockState(), 3);
                }
            }
        }
        
        // 添加水源
        level.setBlock(center, Blocks.WATER.defaultBlockState(), 3);
        
        // 添加农舍
        generateHouse(level, center.offset(10, 0, 0));
    }
    
    private void generateBlacksmith(ServerLevel level, BlockPos center) {
        generateHouse(level, center);
        
        // 添加熔炉和铁砧
        level.setBlock(center.offset(1, 1, 1), Blocks.FURNACE.defaultBlockState(), 3);
        level.setBlock(center.offset(-1, 1, 1), Blocks.ANVIL.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, -1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
    }
    
    private void generateLibrary(ServerLevel level, BlockPos center) {
        generateHouse(level, center);
        
        // 添加书架
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    if (level.getBlockState(center.offset(x, 1, z)).isAir()) {
                        level.setBlock(center.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                        level.setBlock(center.offset(x, 2, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // 添加附魔台
        level.setBlock(center, Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
    }
    
    private void generateChurch(ServerLevel level, BlockPos center) {
        // 生成教堂
        for (int x = -4; x <= 4; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = 0; y <= 8; y++) {
                    BlockPos buildPos = center.offset(x, y, z);
                    
                    if (y == 0) {
                        level.setBlock(buildPos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else if (y == 8 || Math.abs(x) == 4 || Math.abs(z) == 6) {
                        level.setBlock(buildPos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        level.setBlock(buildPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // 添加尖塔
        for (int y = 9; y <= 15; y++) {
            level.setBlock(center.offset(0, y, -5), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        
        // 添加入口
        level.setBlock(center.offset(0, 1, 6), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 2, 6), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 3, 6), Blocks.AIR.defaultBlockState(), 3);
    }
    
    private void addVillageDecorations(ServerLevel level, BlockPos center) {
        // 添加路灯
        BlockPos[] lampPositions = {
            center.offset(20, 0, 20),
            center.offset(-20, 0, 20),
            center.offset(20, 0, -20),
            center.offset(-20, 0, -20),
            center.offset(0, 0, 30),
            center.offset(0, 0, -30),
            center.offset(30, 0, 0),
            center.offset(-30, 0, 0)
        };
        
        for (BlockPos lampPos : lampPositions) {
            // 路灯柱
            for (int y = 1; y <= 4; y++) {
                level.setBlock(lampPos.offset(0, y, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
            }
            level.setBlock(lampPos.offset(0, 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        }
        
        // 添加花园
        for (int i = 0; i < 20; i++) {
            int x = level.random.nextInt(50) - 25;
            int z = level.random.nextInt(50) - 25;
            BlockPos flowerPos = center.offset(x, 1, z);
            
            if (level.getBlockState(flowerPos.below()).getBlock() == Blocks.GRASS_BLOCK &&
                level.getBlockState(flowerPos).isAir()) {
                
                Block flower = level.random.nextBoolean() ? Blocks.POPPY : Blocks.DANDELION;
                level.setBlock(flowerPos, flower.defaultBlockState(), 3);
            }
        }
    }
    
    private void createVillageRoads(ServerLevel level, BlockPos center, BlockPos[] villagePositions) {
        // 创建连接各个村庄的道路
        for (BlockPos villagePos : villagePositions) {
            createRoad(level, center, villagePos);
        }
        
        // 创建村庄之间的环形道路
        for (int i = 0; i < villagePositions.length; i++) {
            BlockPos from = villagePositions[i];
            BlockPos to = villagePositions[(i + 1) % villagePositions.length];
            createRoad(level, from, to);
        }
    }
    
    private void createRoad(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        
        for (int i = 0; i <= steps; i++) {
            int x = from.getX() + (dx * i) / steps;
            int z = from.getZ() + (dz * i) / steps;
            
            // 找到地面高度
            for (int y = 150; y > 60; y--) {
                BlockPos roadPos = new BlockPos(x, y, z);
                if (!level.getBlockState(roadPos).isAir()) {
                    level.setBlock(roadPos, Blocks.DIRT_PATH.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 传送门粒子效果
        for (int i = 0; i < 4; ++i) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            
            double motionX = (random.nextDouble() - 0.5D) * 0.5D;
            double motionY = (random.nextDouble() - 0.5D) * 0.5D;
            double motionZ = (random.nextDouble() - 0.5D) * 0.5D;
            
            level.addParticle(ParticleTypes.PORTAL, x, y, z, motionX, motionY, motionZ);
        }
        
        // 额外的魔法粒子
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, 
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2,
                pos.getY() + 0.5 + random.nextDouble(),
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2,
                0, 0.1, 0);
        }
    }
}