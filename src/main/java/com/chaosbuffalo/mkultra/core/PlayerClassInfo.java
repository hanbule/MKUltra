package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.core.talents.BaseTalent;
import com.chaosbuffalo.mkultra.core.talents.RangedAttributeTalent;
import com.chaosbuffalo.mkultra.core.talents.TalentTree;
import com.chaosbuffalo.mkultra.core.talents.TalentTreeRecord;
import com.chaosbuffalo.mkultra.log.Log;
import com.google.common.collect.Maps;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;
import java.util.*;

public class PlayerClassInfo {
    private ResourceLocation classId;
    private PlayerClass classObj;
    private int level;
    private int unspentPoints;
    private int totalTalentPoints;
    private int unspentTalentPoints;
    private HashMap<ResourceLocation, TalentTreeRecord> talentTrees;
    private ResourceLocation[] hotbar;
    private ResourceLocation[] abilitySpendOrder;
    private ResourceLocation[] loadedPassives;
    private ResourceLocation[] loadedUltimates;

    public PlayerClassInfo(ResourceLocation classId) {
        this.classId = classId;
        this.level = 1;
        this.unspentPoints = 1;
        this.totalTalentPoints = 0;
        this.unspentTalentPoints = 0;
        this.classObj = MKURegistry.getClass(classId);
        loadedPassives = new ResourceLocation[GameConstants.MAX_PASSIVES];
        Arrays.fill(loadedPassives, MKURegistry.INVALID_ABILITY);
        loadedUltimates = new ResourceLocation[GameConstants.MAX_ULTIMATES];
        Arrays.fill(loadedUltimates, MKURegistry.INVALID_ABILITY);
        hotbar = new ResourceLocation[GameConstants.ACTION_BAR_SIZE];
        Arrays.fill(hotbar, MKURegistry.INVALID_ABILITY);
        abilitySpendOrder = new ResourceLocation[GameConstants.MAX_CLASS_LEVEL];
        Arrays.fill(abilitySpendOrder, MKURegistry.INVALID_ABILITY);
        talentTrees = new HashMap<>();
        for (TalentTree tree : MKURegistry.REGISTRY_TALENT_TREES.getValuesCollection()) {
            talentTrees.put(tree.getRegistryName(), new TalentTreeRecord(tree));
        }
    }

    public ResourceLocation getClassId() {
        return classId;
    }

    public int getLevel() {
        return level;
    }

    public int getUnspentPoints() {
        return unspentPoints;
    }

    public int getTotalTalentPoints() {
        return totalTalentPoints;
    }

    public int getUnspentTalentPoints() {
        return unspentTalentPoints;
    }

    void save(PlayerData data) {
        level = data.getLevel();
        unspentPoints = data.getUnspentPoints();
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            hotbar[i] = data.getAbilityInSlot(i);
        }
    }

    boolean checkTalentTotals() {
        int spent = getTotalSpentPoints();
        if (getTotalTalentPoints() - spent != getUnspentTalentPoints()) {
            unspentTalentPoints = getTotalTalentPoints() - spent;
            return true;
        }
        return false;
    }

    private ResourceLocation[] parseNBTAbilityArray(NBTTagCompound tag, String name, int size) {
        NBTTagList list = tag.getTagList(name, Constants.NBT.TAG_STRING);
        ResourceLocation[] arr = new ResourceLocation[size];
        Arrays.fill(arr, MKURegistry.INVALID_ABILITY);
        for (int i = 0; i < size && i < list.tagCount(); i++) {
            arr[i] = new ResourceLocation(list.getStringTagAt(i));
        }
        return arr;
    }

    public void applyPassives(EntityPlayer player, IPlayerData data, World world) {
        Log.debug("applyPassives - loadedPassives %s %s", loadedPassives[0], loadedPassives[1]);
        for (ResourceLocation loc : loadedPassives) {
            if (!loc.equals(MKURegistry.INVALID_ABILITY)) {
                PlayerAbility ability = MKURegistry.getAbility(loc);
                if (ability != null) {
                    ability.execute(player, data, world);
                }
            }
        }
    }

    public boolean hasUltimate(){
        return getUltimateAbilitiesFromTalents().size() > 0;
    }

    public boolean addPassiveToSlot(ResourceLocation loc, int slotIndex) {
        if (canAddPassiveToSlot(loc, slotIndex)) {
            for (int i = 0; i < GameConstants.MAX_PASSIVES; i++) {
                if (!loc.equals(MKURegistry.INVALID_ABILITY) && i != slotIndex && loc.equals(loadedPassives[i])) {
                    loadedPassives[i] = loadedPassives[slotIndex];
                }
            }
            loadedPassives[slotIndex] = loc;
            return true;
        }
        return false;
    }

    public boolean addUltimateToSlot(ResourceLocation loc, int slotIndex){
        if (canAddUltimateToSlot(loc, slotIndex)){
            loadedUltimates[slotIndex] = loc;
            return true;
        }
        return false;
    }

    public int getUltimateSlot(ResourceLocation loc){
        for (int i = 0; i < GameConstants.MAX_ULTIMATES; i++) {
            if (loc.equals(loadedUltimates[i])) {
                return i;
            }
        }

        return GameConstants.ULTIMATE_INVALID_SLOT;
    }


    public int getPassiveSlot(ResourceLocation loc) {
        for (int i = 0; i < GameConstants.MAX_PASSIVES; i++) {
            if (loc.equals(loadedPassives[i])) {
                return i;
            }
        }

        return GameConstants.PASSIVE_INVALID_SLOT;
    }

    public void clearUltimateSlot(int slotIndex){ loadedUltimates[slotIndex] = MKURegistry.INVALID_ABILITY; }

    public void clearPassiveSlot(int slotIndex) {
        loadedPassives[slotIndex] = MKURegistry.INVALID_ABILITY;
    }

    public ResourceLocation getPassiveForSlot(int slotIndex) {
        if (slotIndex >= GameConstants.MAX_PASSIVES) {
            return MKURegistry.INVALID_ABILITY;
        }
        return loadedPassives[slotIndex];
    }

    public ResourceLocation getUltimateForSlot(int slotIndex){
        if (slotIndex >= GameConstants.MAX_ULTIMATES){
            return MKURegistry.INVALID_ABILITY;
        }
        return loadedUltimates[slotIndex];
    }

    public boolean canAddUltimateToSlot(ResourceLocation loc, int slotIndex){
        return slotIndex < GameConstants.MAX_ULTIMATES && hasTrainedUltimate(loc);
    }

    public boolean canAddPassiveToSlot(ResourceLocation loc, int slotIndex) {
        return slotIndex < GameConstants.MAX_PASSIVES && hasTrainedPassive(loc);
    }

    public HashSet<PlayerAbility> getUltimateAbilitiesFromTalents(){
        HashSet<PlayerAbility> abilities = new HashSet<>();
        for (TalentTreeRecord rec: talentTrees.values()){
            if (rec.hasPointsInTree()){
                rec.getUltimatesWithPoints().forEach(talent -> abilities.add(talent.getAbility()));
            }
        }
        return abilities;
    }

    public HashSet<PlayerPassiveAbility> getPassiveAbilitiesFromTalents() {
        HashSet<PlayerPassiveAbility> abilities = new HashSet<>();
        for (TalentTreeRecord rec : talentTrees.values()) {
            if (rec.hasPointsInTree()) {
                rec.getPassivesWithPoints().forEach(talent -> abilities.add(talent.getAbility()));
            }
        }
        return abilities;
    }

    public boolean hasTrainedUltimate(ResourceLocation loc){
        if (loc.equals(MKURegistry.INVALID_ABILITY)){
            return true;
        }
        HashSet<PlayerAbility> abilities = getUltimateAbilitiesFromTalents();
        PlayerAbility ability = MKURegistry.getAbility(loc);
        return ability != null && abilities.contains(ability);
    }

    public boolean hasTrainedPassive(ResourceLocation loc) {
        if (loc.equals(MKURegistry.INVALID_ABILITY)) {
            return true;
        }
        HashSet<PlayerPassiveAbility> abilities = getPassiveAbilitiesFromTalents();
        PlayerAbility ability = MKURegistry.getAbility(loc);
        return ability instanceof PlayerPassiveAbility && abilities.contains(ability);
    }

    public HashSet<RangedAttributeTalent> getAttributeTalentSet() {
        HashSet<RangedAttributeTalent> attributeTalents = new HashSet<>();
        for (TalentTreeRecord rec : talentTrees.values()) {
            if (rec.hasPointsInTree()) {
                attributeTalents.addAll(rec.getAttributeTalentsWithPoints());
            }
        }
        return attributeTalents;
    }

    public Map<IAttribute, AttributeModifier> getAttributeModifiers() {
        HashSet<RangedAttributeTalent> presentTalents = getAttributeTalentSet();
        Map<IAttribute, AttributeModifier> attributeModifierMap = Maps.newHashMap();
        for (RangedAttributeTalent talent : presentTalents) {
            double value = 0.0;
            for (TalentTreeRecord rec : talentTrees.values()) {
                if (rec.hasPointsInTree()) {
                    value += rec.getTotalForAttributeTalent(talent);
                }
            }
//            Log.info("Total for attribute talent: %s, %f", talent.getRegistryName().toString(), value);
            attributeModifierMap.put(talent.getAttribute(), talent.createModifier(value));
        }
        return attributeModifierMap;
    }

    public void applyAttributesModifiersToPlayer(EntityPlayer player) {
        AbstractAttributeMap attributeMap = player.getAttributeMap();

        for (Map.Entry<IAttribute, AttributeModifier> entry : getAttributeModifiers().entrySet()) {
            IAttributeInstance instance = attributeMap.getAttributeInstance(entry.getKey());
            if (instance != null) {
                AttributeModifier attributemodifier = entry.getValue();
                instance.removeModifier(attributemodifier);
                instance.applyModifier(attributemodifier);
            }
        }
    }

    public void removeAttributesModifiersFromPlayer(EntityPlayer player) {
        AbstractAttributeMap attributeMap = player.getAttributeMap();

        for (RangedAttributeTalent entry : getAttributeTalentSet()) {
            IAttributeInstance instance = attributeMap.getAttributeInstance(entry.getAttribute());
            if (instance != null) {
                instance.removeModifier(entry.getUUID());
            }
        }
    }

    private void writeNBTAbilityArray(NBTTagCompound tag, String name, Collection<ResourceLocation> array, int size) {
        NBTTagList list = new NBTTagList();
        if (array != null) {
            array.stream().limit(size).forEach(r -> list.appendTag(new NBTTagString(r.toString())));
        }
        tag.setTag(name, list);
    }

    public void writeTalentTrees(NBTTagCompound tag) {
        NBTTagCompound trees = new NBTTagCompound();
        boolean hadTalents = false;
        for (ResourceLocation loc : talentTrees.keySet()) {
            TalentTreeRecord record = talentTrees.get(loc);
            if (record.hasPointsInTree()) {
                trees.setTag(loc.toString(), record.toTag());
                hadTalents = true;
            }
        }
        if (hadTalents) {
            tag.setTag("trees", trees);
        }
    }

    public void parseTalentTrees(NBTTagCompound tag) {
        boolean doReset = false;
        if (tag.hasKey("trees")) {
            NBTTagCompound trees = tag.getCompoundTag("trees");
            for (String key : trees.getKeySet()) {
                ResourceLocation loc = new ResourceLocation(key);
                if (talentTrees.containsKey(loc)) {
                    boolean needsReset = talentTrees.get(loc).fromTag(trees.getCompoundTag(key));
                    if (needsReset) {
                        doReset = true;
                    }
                }
            }
        }
        if (doReset){
            clearUltimateAbilities();
            clearPassiveAbilities();
        }
    }

    public void serialize(NBTTagCompound tag) {
        tag.setString("id", classId.toString());
        tag.setInteger("level", level);
        tag.setInteger("classAbilityHash", classObj.hashAbilities());
        tag.setInteger("unspentPoints", unspentPoints);
        writeNBTAbilityArray(tag, "abilitySpendOrder", Arrays.asList(abilitySpendOrder), GameConstants.MAX_CLASS_LEVEL);
        writeNBTAbilityArray(tag, "hotbar", Arrays.asList(hotbar), GameConstants.ACTION_BAR_SIZE);
        serializeTalentInfo(tag);
    }

    public void deserialize(NBTTagCompound tag) {
        classId = new ResourceLocation(tag.getString("id"));
        classObj = MKURegistry.getClass(classId);
        level = tag.getInteger("level");
        if (tag.hasKey("classAbilityHash")){
            int abilityHash = tag.getInteger("classAbilityHash");
            if (abilityHash == classObj.hashAbilities()){
                unspentPoints = tag.getInteger("unspentPoints");
                abilitySpendOrder = parseNBTAbilityArray(tag, "abilitySpendOrder", GameConstants.MAX_CLASS_LEVEL);
                setActiveAbilities(parseNBTAbilityArray(tag, "hotbar", GameConstants.ACTION_BAR_SIZE));
            } else {
                unspentPoints = level;
                clearAbilitySpendOrder();
                clearActiveAbilities();
            }
        } else {
            unspentPoints = level;
            clearAbilitySpendOrder();
            clearActiveAbilities();
        }

        deserializeTalentInfo(tag);
    }

    public void serializeTalentInfo(NBTTagCompound tag) {
        tag.setInteger("unspentTalentPoints", unspentTalentPoints);
        tag.setInteger("totalTalentPoints", totalTalentPoints);
        writeNBTAbilityArray(tag, "loadedPassives", Arrays.asList(loadedPassives), GameConstants.MAX_PASSIVES);
        writeNBTAbilityArray(tag, "loadedUltimates", Arrays.asList(loadedUltimates), GameConstants.MAX_ULTIMATES);
        writeTalentTrees(tag);
    }

    public void deserializeTalentInfo(NBTTagCompound tag) {
        unspentTalentPoints = tag.getInteger("unspentTalentPoints");
        totalTalentPoints = tag.getInteger("totalTalentPoints");
        if (tag.hasKey("loadedPassives")) {
            setLoadedPassives(parseNBTAbilityArray(tag, "loadedPassives", GameConstants.MAX_PASSIVES));
        }
        if (tag.hasKey("loadedUltimates")){
            setLoadedUltimates(parseNBTAbilityArray(tag, "loadedUltimates", GameConstants.MAX_ULTIMATES));
        }
        parseTalentTrees(tag);
    }

    public ResourceLocation[] getActivePassives() {
        return loadedPassives;
    }

    public ResourceLocation[] getActiveUltimates(){
        return loadedUltimates;
    }

    public ResourceLocation[] getActiveAbilities() {
        return hotbar;
    }

    public void addTalentPoints(int pointCount) {
        totalTalentPoints += pointCount;
        unspentTalentPoints += pointCount;
    }

    public boolean canIncrementPointInTree(ResourceLocation tree, String line, int index) {
        if (getUnspentTalentPoints() == 0)
            return false;
        TalentTreeRecord talentTree = talentTrees.get(tree);
        return talentTree != null && talentTree.canIncrementPoint(line, index);
    }

    public boolean canDecrementPointInTree(ResourceLocation tree, String line, int index) {
        TalentTreeRecord talentTree = talentTrees.get(tree);
        return talentTree != null && talentTree.canDecrementPoint(line, index);
    }

    public boolean spendTalentPoint(EntityPlayer player, ResourceLocation tree, String line, int index) {
        if (canIncrementPointInTree(tree, line, index)) {
            TalentTreeRecord talentTree = talentTrees.get(tree);
            BaseTalent talentDef = talentTree.getTalentDefinition(line, index);
            if (talentDef.onAdd(player, this)) {
                talentTree.incrementPoint(line, index);
                unspentTalentPoints -= 1;
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    public int getTotalSpentPoints() {
        int tot = 0;
        for (TalentTreeRecord talentTree : talentTrees.values()) {
            tot += talentTree.getPointsInTree();
        }
        return tot;
    }

    public boolean refundTalentPoint(EntityPlayer player, ResourceLocation tree, String line, int index) {
        if (canDecrementPointInTree(tree, line, index)) {
            TalentTreeRecord talentTree = talentTrees.get(tree);
            BaseTalent talentDef = talentTree.getTalentDefinition(line, index);
            if (talentDef.onRemove(player, this)) {
                talentTree.decrementPoint(line, index);
                unspentTalentPoints += 1;
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    void setLoadedUltimates(ResourceLocation[] ultimates){
        this.loadedUltimates = ultimates;
    }

    void setLoadedPassives(ResourceLocation[] passives) {
        this.loadedPassives = passives;
    }

    void setActiveAbilities(ResourceLocation[] hotbar) {
        this.hotbar = hotbar;
    }

    public void setAbilitySpendOrder(ResourceLocation abilityId, int level) {
        if (level > 0) {
            abilitySpendOrder[level - 1] = abilityId;
        }
    }

    public void clearUltimateAbilities(){
        Arrays.fill(loadedUltimates, MKURegistry.INVALID_ABILITY);
    }

    public void clearPassiveAbilities(){
        Arrays.fill(loadedPassives, MKURegistry.INVALID_ABILITY);
    }

    public void clearActiveAbilities(){
        Arrays.fill(hotbar, MKURegistry.INVALID_ABILITY);
    }

    public void clearAbilitySpendOrder() {
        Arrays.fill(abilitySpendOrder, MKURegistry.INVALID_ABILITY);
    }

    public ResourceLocation getAbilitySpendOrder(int index) {
        ResourceLocation id = MKURegistry.INVALID_ABILITY;
        if (index > 0) {
            id = abilitySpendOrder[index - 1];
            abilitySpendOrder[index - 1] = MKURegistry.INVALID_ABILITY;
        }
        return id;
    }

    public TalentTreeRecord getTalentTree(ResourceLocation loc) {
        return talentTrees.get(loc);
    }


    public static PlayerClassInfo deserializeUpdate(PacketBuffer pb) {
        PlayerClassInfo info = new PlayerClassInfo(pb.readResourceLocation());

        info.level = pb.readInt();
        try {
            NBTTagCompound talentData = pb.readCompoundTag();
            info.deserializeTalentInfo(talentData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return info;
    }

    public void serializeUpdate(PacketBuffer pb) {
        pb.writeResourceLocation(getClassId());
        pb.writeInt(getLevel());
        NBTTagCompound talentData = new NBTTagCompound();
        serializeTalentInfo(talentData);
        pb.writeCompoundTag(talentData);
    }
}
