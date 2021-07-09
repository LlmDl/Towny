package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.PermissionUtil.SetPermissionType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PermissionData {
	private SetPermissionType[] permissionTypes;
	private long lastChangedAt;
	private String lastChangedBy;
	
	public PermissionData(SetPermissionType[] permissionTypes, String changedBy) {
		this.permissionTypes = permissionTypes;
		this.lastChangedBy = changedBy;
		this.lastChangedAt = System.currentTimeMillis();
	}

	public PermissionData(String string) {
		String[] data = string.split(",");

		long lastChanged = Long.parseLong(data[1]);
		SetPermissionType[] types = new SetPermissionType[4];

		for (int i = 0; i < 4; i++) {
			types[i] = SetPermissionType.valueOf(data[i+2]);
		}
		
		this.permissionTypes = types;
		this.lastChangedAt = lastChanged;
		this.lastChangedBy = data[0];
	}

	public SetPermissionType[] getPermissionTypes() {
		return permissionTypes;
	}

	public String getLastChangedBy() {
		return lastChangedBy;
	}

	public long getLastChangedAt() {
		return lastChangedAt;
	}

	public void setLastChangedBy(String changedBy) {
		this.lastChangedBy = changedBy;
	}

	public void setLastChangedAt(long lastChangedAt) {
		this.lastChangedAt = lastChangedAt;
	}

	public void setPermissionTypes(SetPermissionType[] permissionTypes) {
		this.permissionTypes = permissionTypes;
	}
	
	@Override
	public String toString() {
		return lastChangedBy + "," + lastChangedAt + "," + Arrays.stream(permissionTypes).map(SetPermissionType::name).collect(Collectors.joining(","));
	}
}
