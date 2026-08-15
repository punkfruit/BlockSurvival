package com.daniel.blocksurvival.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.ItemStack;
import com.daniel.blocksurvival.inventory.Items;
import com.daniel.blocksurvival.Hotbar;

import java.util.ArrayList;
import java.util.List;

public class SaveManager {

    /*
     * These four bytes identify the file as one of our
     * Block Survival chunk files.
     *
     * In hexadecimal, this becomes:
     *
     * 42 53 43 48
     *
     * Those bytes represent the ASCII letters:
     *
     * B S C H
     */
    private static final int CHUNK_FILE_MAGIC =
            0x42534348;

    /*
     * If we change the save-file structure later,
     * we can increase this number.
     */
    private static final int CHUNK_FILE_VERSION = 2;

    /*
     * We reserve zero for empty space.
     *
     * Every actual BlockType is stored as:
     *
     * enum ordinal + 1
     */
    private static final int EMPTY_BLOCK_ID = 0;

    private final Path chunksDirectory;
    private final Path worldDirectory;

    private static final int PLAYER_FILE_MAGIC =
            0x4253504C;

    private static final int PLAYER_FILE_VERSION = 3;



    public SaveManager(
            String worldName
    ) {
        worldDirectory =
                Path.of(
                        "saves",
                        worldName
                );

        chunksDirectory =
                worldDirectory.resolve(
                        "chunks"
                );

        createSaveDirectories();
    }

    private void createSaveDirectories() {
        try {
            Files.createDirectories(
                    chunksDirectory
            );
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to create the save directory: "
                            + chunksDirectory,
                    exception
            );
        }
    }

    public void saveChunk(
            Chunk chunk
    ) {
        Path chunkFile =
                getChunkFilePath(
                        chunk.getChunkX(),
                        chunk.getChunkY(),
                        chunk.getChunkZ()
                );

        try (
                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                chunkFile
                                        )
                                )
                        )
        ) {
            writeHeader(
                    output,
                    chunk
            );

            writeBlocks(
                    output,
                    chunk
            );

            writeBlockDirections(
                    output,
                    chunk
            );

            /*
             * The chunk now matches the copy on disk.
             */
            chunk.clearDirty();

            System.out.println(
                    "Saved chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to save chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            exception.printStackTrace();
        }
    }

    public boolean loadChunk(
            Chunk chunk
    ) {
        Path chunkFile =
                getChunkFilePath(
                        chunk.getChunkX(),
                        chunk.getChunkY(),
                        chunk.getChunkZ()
                );

        /*
         * False means there was no saved version.
         *
         * That is completely normal for a newly explored chunk.
         */
        if (!Files.exists(chunkFile)) {
            return false;
        }

        try (
                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                chunkFile
                                        )
                                )
                        )
        ) {
            readAndValidateHeader(
                    input,
                    chunk
            );

            readBlocks(
                    input,
                    chunk
            );

            readBlockDirections(
                    input,
                    chunk
            );

            /*
             * Loading calls Chunk.setBlock(), which temporarily
             * marks the chunk dirty.
             *
             * Once loading is complete, the chunk exactly matches
             * the file, so it is no longer dirty.
             */
            chunk.clearDirty();
            chunk.setState(
                    ChunkState.GENERATED
            );

            System.out.println(
                    "Loaded chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            return true;
        }
        catch (EOFException exception) {
            System.err.println(
                    "Chunk file ended unexpectedly: "
                            + chunkFile
            );

            return false;
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to load chunk file: "
                            + chunkFile
            );

            exception.printStackTrace();

            return false;
        }
    }

    public boolean hasSavedChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return Files.exists(
                getChunkFilePath(
                        chunkX,
                        chunkY,
                        chunkZ
                )
        );
    }

    private void writeHeader(
            DataOutputStream output,
            Chunk chunk
    ) throws IOException {

        output.writeInt(
                CHUNK_FILE_MAGIC
        );

        output.writeInt(
                CHUNK_FILE_VERSION
        );

        output.writeInt(
                chunk.getChunkX()
        );

        output.writeInt(
                chunk.getChunkY()
        );

        output.writeInt(
                chunk.getChunkZ()
        );
    }

    private void readAndValidateHeader(
            DataInputStream input,
            Chunk chunk
    ) throws IOException {

        int magic =
                input.readInt();

        if (magic != CHUNK_FILE_MAGIC) {
            throw new IOException(
                    "File is not a valid Block Survival chunk."
            );
        }

        int version =
                input.readInt();

        if (version != CHUNK_FILE_VERSION) {
            throw new IOException(
                    "Unsupported chunk version: "
                            + version
            );
        }

        int savedChunkX =
                input.readInt();

        int savedChunkY =
                input.readInt();

        int savedChunkZ =
                input.readInt();

        if (
                savedChunkX != chunk.getChunkX() ||
                        savedChunkY != chunk.getChunkY() ||
                        savedChunkZ != chunk.getChunkZ()
        ) {
            throw new IOException(
                    "Chunk coordinates inside the file do not "
                            + "match its requested location."
            );
        }
    }

    private void writeBlocks(
            DataOutputStream output,
            Chunk chunk
    ) throws IOException {

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {

                    BlockType block =
                            chunk.getBlock(
                                    x,
                                    y,
                                    z
                            );

                    int savedBlockId =
                            getSavedBlockId(
                                    block
                            );

                    /*
                     * One unsigned byte supports values
                     * from 0 through 255.
                     */
                    output.writeByte(
                            savedBlockId
                    );
                }
            }
        }
    }

    private void readBlocks(
            DataInputStream input,
            Chunk chunk
    ) throws IOException {

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {

                    /*
                     * readUnsignedByte returns 0–255 rather
                     * than Java's signed byte range.
                     */
                    int savedBlockId =
                            input.readUnsignedByte();

                    BlockType block =
                            getBlockType(
                                    savedBlockId
                            );

                    chunk.setBlock(
                            x,
                            y,
                            z,
                            block
                    );
                }
            }
        }
    }


    private void writeBlockDirections(
            DataOutputStream output,
            Chunk chunk
    ) throws IOException {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    BlockDirection direction =
                            chunk.getBlockDirection(
                                    x,
                                    y,
                                    z
                            );

                    output.writeByte(
                            direction.ordinal()
                    );
                }
            }
        }
    }

    private void readBlockDirections(
            DataInputStream input,
            Chunk chunk
    ) throws IOException {
        BlockDirection[] directions =
                BlockDirection.values();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    int directionOrdinal =
                            input.readUnsignedByte();

                    if (
                            directionOrdinal < 0 ||
                                    directionOrdinal >=
                                            directions.length
                    ) {
                        throw new IOException(
                                "Unknown block direction: "
                                        + directionOrdinal
                        );
                    }

                    chunk.setBlockDirection(
                            x,
                            y,
                            z,
                            directions[
                                    directionOrdinal
                                    ]
                    );
                }
            }
        }
    }

    private int getSavedBlockId(
            BlockType block
    ) {
        if (block == null) {
            return EMPTY_BLOCK_ID;
        }

        int savedBlockId =
                block.ordinal() + 1;

        if (savedBlockId > 255) {
            throw new IllegalStateException(
                    "Too many BlockType values for the "
                            + "current one-byte save format."
            );
        }

        return savedBlockId;
    }

    private BlockType getBlockType(
            int savedBlockId
    ) throws IOException {

        if (savedBlockId == EMPTY_BLOCK_ID) {
            return null;
        }

        int blockOrdinal =
                savedBlockId - 1;

        BlockType[] blockTypes =
                BlockType.values();

        if (
                blockOrdinal < 0 ||
                        blockOrdinal >= blockTypes.length
        ) {
            throw new IOException(
                    "Unknown saved block ID: "
                            + savedBlockId
            );
        }

        return blockTypes[blockOrdinal];
    }

    private Path getChunkFilePath(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        String fileName =
                chunkX
                        + "_"
                        + chunkY
                        + "_"
                        + chunkZ
                        + ".chunk";

        return chunksDirectory.resolve(
                fileName
        );
    }

    public void savePlayer(
            PlayerSaveData playerData,
            Inventory inventory,
            Hotbar hotbar
    ) {
        Path playerFile =
                worldDirectory.resolve(
                        "player.dat"
                );

        try (
                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                playerFile
                                        )
                                )
                        )
        ) {
            output.writeInt(
                    PLAYER_FILE_MAGIC
            );

            output.writeInt(
                    PLAYER_FILE_VERSION
            );

            output.writeFloat(
                    playerData.positionX()
            );

            output.writeFloat(
                    playerData.positionY()
            );

            output.writeFloat(
                    playerData.positionZ()
            );

            output.writeFloat(
                    playerData.yaw()
            );

            output.writeFloat(
                    playerData.pitch()
            );

            output.writeInt(
                    playerData.selectedHotbarSlot()
            );

            writeInventory(
                    output,
                    inventory
            );

            writeHotbar(
                    output,
                    hotbar
            );

            System.out.println(
                    "Saved player."
            );
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to save player."
            );

            exception.printStackTrace();
        }
    }

    private void writeHotbar(
            DataOutputStream output,
            Hotbar hotbar
    ) throws IOException {
        output.writeInt(
                hotbar.getSlotCount()
        );

        for (
                int slotIndex = 0;
                slotIndex < hotbar.getSlotCount();
                slotIndex++
        ) {
            ItemDefinition item =
                    hotbar.getItem(
                            slotIndex
                    );

            /*
             * Write whether this slot is occupied first.
             *
             * That avoids needing a fake string such as "null".
             */
            boolean occupied =
                    item != null;

            output.writeBoolean(
                    occupied
            );

            if (occupied) {
                output.writeUTF(
                        item.id()
                );
            }
        }
    }

    public PlayerSaveData loadPlayer(
            Inventory inventory,
            Hotbar hotbar
    ) {
        Path playerFile =
                worldDirectory.resolve(
                        "player.dat"
                );

        if (!Files.exists(playerFile)) {
            return null;
        }

        try (
                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                playerFile
                                        )
                                )
                        )
        ) {
            int magic =
                    input.readInt();

            if (magic != PLAYER_FILE_MAGIC) {
                throw new IOException(
                        "File is not a valid Block Survival player save."
                );
            }

            int version =
                    input.readInt();

            if (
                    version < 1 ||
                            version >
                                    PLAYER_FILE_VERSION
            ) {
                throw new IOException(
                        "Unsupported player save version: "
                                + version
                );
            }

            float positionX =
                    input.readFloat();

            float positionY =
                    input.readFloat();

            float positionZ =
                    input.readFloat();

            float yaw =
                    input.readFloat();

            float pitch =
                    input.readFloat();

            int selectedHotbarSlot =
                    input.readInt();

            if (version >= 2) {
                readInventory(
                        input,
                        inventory
                );
            }
            else {
                /*
                 * Version 1 had no saved inventory.
                 */
                inventory.clear();
            }

            if (version >= 3) {
                readHotbar(
                        input,
                        hotbar
                );
            }
            else {
                clearHotbar(
                        hotbar
                );
            }

            System.out.println(
                    "Loaded player."
            );

            return new PlayerSaveData(
                    positionX,
                    positionY,
                    positionZ,
                    yaw,
                    pitch,
                    selectedHotbarSlot
            );
        }
        catch (EOFException exception) {
            System.err.println(
                    "Player save ended unexpectedly."
            );

            return null;
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to load player."
            );

            exception.printStackTrace();

            return null;
        }
    }
    private void writeInventory(
            DataOutputStream output,
            Inventory inventory
    ) throws IOException {
        output.writeInt(
                inventory.getWidth()
        );

        output.writeInt(
                inventory.getHeight()
        );

        output.writeInt(
                inventory.getStacks()
                        .size()
        );

        for (
                ItemStack stack :
                inventory.getStacks()
        ) {
            output.writeUTF(
                    stack.getDefinition()
                            .id()
            );

            output.writeInt(
                    stack.getQuantity()
            );

            output.writeInt(
                    stack.getGridX()
            );

            output.writeInt(
                    stack.getGridY()
            );

            output.writeBoolean(
                    stack.isRotated()
            );
        }
    }
    private void readInventory(
            DataInputStream input,
            Inventory inventory
    ) throws IOException {
        int savedWidth =
                input.readInt();

        int savedHeight =
                input.readInt();

        if (
                savedWidth !=
                        inventory.getWidth() ||
                        savedHeight !=
                                inventory.getHeight()
        ) {
            throw new IOException(
                    "Saved inventory size " +
                            savedWidth +
                            "x" +
                            savedHeight +
                            " does not match current inventory size " +
                            inventory.getWidth() +
                            "x" +
                            inventory.getHeight() +
                            "."
            );
        }

        int stackCount =
                input.readInt();

        if (
                stackCount < 0 ||
                        stackCount >
                                savedWidth *
                                        savedHeight
        ) {
            throw new IOException(
                    "Invalid inventory stack count: " +
                            stackCount
            );
        }

        List<SavedInventoryStack> savedStacks =
                new ArrayList<>();

        for (
                int index = 0;
                index < stackCount;
                index++
        ) {
            String itemId =
                    input.readUTF();

            int quantity =
                    input.readInt();

            int gridX =
                    input.readInt();

            int gridY =
                    input.readInt();

            boolean rotated =
                    input.readBoolean();

            ItemDefinition definition =
                    Items.getById(
                            itemId
                    );

            if (definition == null) {
                throw new IOException(
                        "Unknown saved item ID: " +
                                itemId
                );
            }

            savedStacks.add(
                    new SavedInventoryStack(
                            definition,
                            quantity,
                            gridX,
                            gridY,
                            rotated
                    )
            );
        }

        /*
         * Only replace the current inventory after the entire
         * saved inventory was read successfully.
         */
        inventory.clear();

        for (
                SavedInventoryStack savedStack :
                savedStacks
        ) {
            boolean restored =
                    inventory.restoreStack(
                            savedStack.definition(),
                            savedStack.quantity(),
                            savedStack.gridX(),
                            savedStack.gridY(),
                            savedStack.rotated()
                    );

            if (!restored) {
                inventory.clear();

                throw new IOException(
                        "Could not restore item " +
                                savedStack.definition()
                                        .id() +
                                " at [" +
                                savedStack.gridX() +
                                ", " +
                                savedStack.gridY() +
                                "]."
                );
            }
        }

        System.out.println(
                "Loaded player inventory with " +
                        stackCount +
                        " stack(s)."
        );
    }

    private void readHotbar(
            DataInputStream input,
            Hotbar hotbar
    ) throws IOException {
        int savedSlotCount =
                input.readInt();

        if (
                savedSlotCount !=
                        hotbar.getSlotCount()
        ) {
            throw new IOException(
                    "Saved hotbar has " +
                            savedSlotCount +
                            " slots, but current hotbar has " +
                            hotbar.getSlotCount() +
                            "."
            );
        }

        /*
         * Clear everything first so loading completely replaces
         * the current assignments.
         */
        clearHotbar(
                hotbar
        );

        for (
                int slotIndex = 0;
                slotIndex < savedSlotCount;
                slotIndex++
        ) {
            boolean occupied =
                    input.readBoolean();

            if (!occupied) {
                continue;
            }

            String itemId =
                    input.readUTF();

            ItemDefinition item =
                    Items.getById(
                            itemId
                    );

            if (item == null) {
                throw new IOException(
                        "Unknown hotbar item ID: " +
                                itemId
                );
            }

            hotbar.assignSlot(
                    slotIndex,
                    item
            );
        }

        System.out.println(
                "Loaded hotbar assignments."
        );
    }

    private void clearHotbar(
            Hotbar hotbar
    ) {
        for (
                int slotIndex = 0;
                slotIndex < hotbar.getSlotCount();
                slotIndex++
        ) {
            hotbar.clearSlot(
                    slotIndex
            );
        }
    }

    private record SavedInventoryStack(
            ItemDefinition definition,
            int quantity,
            int gridX,
            int gridY,
            boolean rotated
    ) {
    }

}