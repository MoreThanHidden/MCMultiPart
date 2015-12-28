package mcmultipart.multipart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import mcmultipart.MCMultiPartMod;
import mcmultipart.block.TileMultipart;
import mcmultipart.microblock.IMicroblockTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class MultipartHelper {

    public static boolean canAddPart(World world, BlockPos pos, IMultipart part) {

        IMultipartContainer container = getPartContainer(world, pos);
        if (container == null) {
            List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
            part.addCollisionBoxes(new AxisAlignedBB(0, 0, 0, 1, 1, 1), list, null);
            for (AxisAlignedBB bb : list)
                if (!world.checkNoEntityCollision(bb.offset(pos.getX(), pos.getY(), pos.getZ()))) return false;

            Collection<? extends IMultipart> parts = MultipartRegistry.convert(world, pos);
            if (parts != null && !parts.isEmpty()) {
                TileMultipart tmp = new TileMultipart();
                for (IMultipart p : parts)
                    tmp.getPartContainer().addPart(p, false, false, UUID.randomUUID());
                return tmp.canAddPart(part);
            }

            return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
        }
        return container.canAddPart(part);
    }

    public static void addPart(World world, BlockPos pos, IMultipart part) {

        addPart(world, pos, part, null);
    }

    public static void addPart(World world, BlockPos pos, IMultipart part, UUID id) {

        IMultipartContainer container = getOrConvertPartContainer(world, pos);
        boolean newContainer;
        if (newContainer = (container == null)) {
            world.setBlockState(pos, MCMultiPartMod.multipart.getDefaultState());
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileMultipart) container = (IMultipartContainer) te;
            if (container == null) world.setTileEntity(pos, (TileEntity) (container = new TileMultipart()));
        }
        if (container.getPartFromID(id) != null) return;
        part.setContainer(container);
        if (id != null) container.addPart(id, part);
        else container.addPart(part);
        if (newContainer) world.notifyLightSet(pos);
    }

    public static boolean addPartIfPossible(World world, BlockPos pos, IMultipart part) {

        if (!canAddPart(world, pos, part)) return false;
        addPart(world, pos, part);
        return true;
    }

    public static IMultipartContainer getPartContainer(IBlockAccess world, BlockPos pos) {

        TileEntity te = world.getTileEntity(pos);
        if (te == null) return null;
        if (te instanceof IMultipartContainer) return (IMultipartContainer) te;
        if (te instanceof IMicroblockTile) return ((IMicroblockTile) te).getMicroblockContainer();
        return null;
    }

    public static IMultipartContainer getOrConvertPartContainer(World world, BlockPos pos) {

        IMultipartContainer container = getPartContainer(world, pos);
        if (container != null) return container;

        Collection<? extends IMultipart> parts = MultipartRegistry.convert(world, pos);
        if (parts == null || parts.isEmpty()) return null;

        TileEntity oldTile = world.getTileEntity(pos);
        world.setBlockState(pos, MCMultiPartMod.multipart.getDefaultState());
        TileEntity tile = world.getTileEntity(pos);
        TileMultipart te = null;
        if (tile == null || !(tile instanceof TileMultipart)) world.setTileEntity(pos, te = new TileMultipart());
        else te = (TileMultipart) tile;

        for (IMultipart part : parts)
            te.getPartContainer().addPart(part, false, false, UUID.randomUUID());
        for (IMultipart part : parts)
            part.onConverted(oldTile);

        world.notifyLightSet(pos);

        return te;
    }

}