/*
 * Copyright (C) Matthew Steglinski (SainttX) <matt@ipvp.org>
 * Copyright (C) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.ipvp.canvas.type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.ipvp.canvas.ArrayIterator;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.DefaultSlot;
import org.ipvp.canvas.slot.Slot;

/**
 * An abstract class that provides a skeletal implementation of the Menu 
 * interface.
 */
public abstract class AbstractMenu implements Menu  {

    private Menu parent;
    private DefaultSlot[] slots;
    private CloseHandler handler;
    private Set<MenuHolder> viewers = new HashSet<>();

    // Bukkit Inventory information
    protected String inventoryTitle;
    protected int inventorySlots;
    protected InventoryType inventoryType;

    protected AbstractMenu(String title, int inventorySlots, Menu parent) {
        if (title == null) {
            title = InventoryType.CHEST.getDefaultTitle();
        }
        this.inventoryTitle = title;
        this.inventorySlots = inventorySlots;
        this.parent = parent;
        this.generateSlots();
    }
    
    protected AbstractMenu(String title, InventoryType type, Menu parent) {
        Objects.requireNonNull(type, "type cannot be null");
        if (title == null) {
            title = type.getDefaultTitle();
        }
        this.inventoryTitle = title;
        this.inventoryType = type;
        this.parent = parent;
        this.generateSlots();
    }

    /**
     * Initial method called to fill the Slots of the menu
     */
    protected void generateSlots() {
        this.slots = new DefaultSlot[getDimensions().getArea()];
        for (int i = 0 ; i < slots.length ; i++) {
            this.slots[i] = new DefaultSlot(this, i);
        }
    }

    @Override
    public Optional<Menu> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public void open(Player viewer) {
        InventoryHolder currentInventory =
                viewer.getOpenInventory().getTopInventory().getHolder();
        if (currentInventory instanceof MenuHolder) {
            MenuHolder holder = (MenuHolder) currentInventory;
            Menu open = holder.getMenu();

            if (open == this) {
                return;
            }

            open.close(viewer);
            holder.setMenu(this);
            Inventory inventory = createInventory(holder);
            holder.setInventory(inventory);
            updateInventoryContents(viewer, inventory);
            viewer.openInventory(inventory);
            viewers.add(holder);
        } else {
            // Create new MenuHolder for the player
            MenuHolder holder = new MenuHolder(this);
            Inventory inventory = createInventory(holder);
            updateInventoryContents(viewer, inventory);
            holder.setInventory(inventory);
            viewer.openInventory(inventory);
            viewers.add(holder);
        }
    }

    private Inventory createInventory(InventoryHolder holder) {
        return inventoryType == null
                ? Bukkit.createInventory(holder, inventorySlots, inventoryTitle)
                : Bukkit.createInventory(holder, inventoryType, inventoryTitle);
    }

    private void updateInventoryContents(Player viewer, Inventory inventory) {
        for (Slot slot : slots) {
            inventory.setItem(slot.getIndex(), slot.getItem());
        }
        viewer.updateInventory();
    }

    @Override
    public void close(Player viewer) {
        closedByPlayer(viewer);
        viewer.closeInventory();
    }

    public void closedByPlayer(Player viewer) {
        InventoryHolder currentInventory =
                viewer.getOpenInventory().getTopInventory().getHolder();

        if (!(currentInventory instanceof MenuHolder)
                || !viewers.contains(currentInventory)) {
            return;
        }

        MenuHolder holder = (MenuHolder) currentInventory;
        viewers.remove(holder);
        getCloseHandler().ifPresent(h -> h.close(viewer, this));
    }

    public Set<MenuHolder> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    @Override
    public Slot getSlot(int index) {
        return slots[index];
    }

    @Override
    public Iterator<Slot> iterator() {
        return new ArrayIterator<>(slots);
    }

    @Override
    public void clear() {
        for (Slot slot : slots) {
            slot.setItem(null);
        }
    }

    @Override
    public void clear(int index) {
        Slot slot = getSlot(index);
        slot.setItem(null);
    }

    @Override
    public void setCloseHandler(CloseHandler handler) {
        this.handler = handler;
    }

    @Override
    public Optional<CloseHandler> getCloseHandler() {
        return Optional.ofNullable(handler);
    }

    /**
     * Abstract base class for builders of {@link Menu} types.
     * <p>
     * Builder instances are reusable; calling {@link #build()} will
     * generate a new Menu with identical features to the ones created before it.
     */
    public static abstract class Builder implements Menu.Builder {
        
        private String title;
        private Menu parent;

        @Override
        public Menu.Builder title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public Menu.Builder parent(Menu parent) {
            this.parent = parent;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Menu getParent() {
            return parent;
        }
    }
}
