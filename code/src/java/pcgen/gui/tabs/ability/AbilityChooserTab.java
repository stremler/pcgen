/*
 * AbilityChooserTab.java
 * Copyright 2008 Connor Petty <cpmeister@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Created on Jun 29, 2008, 10:30:57 PM
 */
package pcgen.gui.tabs.ability;

import javax.swing.event.ListDataEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import pcgen.gui.PCGenUIManager;
import pcgen.gui.facade.AbilityCatagoryFacade;
import pcgen.gui.facade.AbilityFacade;
import pcgen.gui.facade.CharacterFacade;
import pcgen.gui.tabs.AbstractChooserTab;
import pcgen.gui.tools.FilterableTreeViewModel;
import pcgen.gui.tools.FilteredTreeViewPanel;
import pcgen.gui.util.GenericListModel;
import pcgen.gui.util.JTreeViewPane;
import pcgen.gui.util.event.GenericListDataEvent;
import pcgen.gui.util.event.GenericListDataListener;
import pcgen.gui.util.panes.FlippingSplitPane;
import pcgen.gui.util.treeview.*;

/**
 *
 * @author Connor Petty <cpmeister@users.sourceforge.net>
 */
public class AbilityChooserTab extends AbstractChooserTab
{

    private static final DataView<AbilityFacade> abilityDataView = new DataView<AbilityFacade>()
    {

        private final List<? extends DataViewColumn> dataColumns =
                Arrays.asList(new DefaultDataViewColumn("Type",
                                                        String.class),
                              new DefaultDataViewColumn("Mult",
                                                        Boolean.class),
                              new DefaultDataViewColumn("Stack",
                                                        Boolean.class),
                              new DefaultDataViewColumn("Description",
                                                        String.class),
                              new DefaultDataViewColumn("Source",
                                                        String.class));

        public List<? extends DataViewColumn> getDataColumns()
        {
            return dataColumns;
        }

        public List<?> getData(AbilityFacade obj)
        {
            return Arrays.asList(getTypes(obj.getTypes()),
                                 obj.isMult(),
                                 obj.isStackable(),
                                 obj.getDescription(),
                                 obj.getSource());
        }

        private String getTypes(List<String> types)
        {
            String ret = types.get(0);
            for (int x = 1; x < types.size(); x++)
            {
                ret += ", " + types.get(x);
            }
            return ret;
        }

    };
    private final FilteredTreeViewPanel availableTreeViewPanel;
    private final FilteredTreeViewPanel selectedTreeViewPanel;
    private final JTable catagoryTable;

    public AbilityChooserTab()
    {
        this.availableTreeViewPanel = new FilteredTreeViewPanel();
        this.selectedTreeViewPanel = new FilteredTreeViewPanel();
        this.catagoryTable = new JTable();
        initComponents();
    }

    private void initComponents()
    {

        selectedTreeViewPanel.setRowSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        availableTreeViewPanel.setRowSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        FlippingSplitPane pane = new FlippingSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                       true,
                                                       selectedTreeViewPanel,
                                                       availableTreeViewPanel);
        pane.setOneTouchExpandable(true);
        pane.setDividerSize(7);
        setPrimaryChooserComponent(pane);

        catagoryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane catagoryScrollPane = new JScrollPane(catagoryTable,
                                                         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setSecondaryChooserComponent(catagoryScrollPane);
    }

    private final class SelectedAbilityTreeViewModel implements FilterableTreeViewModel<AbilityFacade>
    {

        private final List<? extends TreeView<AbilityFacade>> treeViews = null;
        private final Map<AbilityFacade, AbilityCatagoryFacade> abilityMap;
        private final GenericListModel<AbilityFacade> model;
        private final CharacterFacade character;

        public SelectedAbilityTreeViewModel(CharacterFacade character)
        {
            this.abilityMap = new HashMap<AbilityFacade, AbilityCatagoryFacade>();
            this.model = new GenericListModel<AbilityFacade>();
            this.character = character;

            AbilityCatagoryFacade[] catagories = PCGenUIManager.getRegisteredAbilityCatagories(character).toArray(new AbilityCatagoryFacade[0]);
            for (final AbilityCatagoryFacade catagory : catagories)
            {
                final GenericListModel<AbilityFacade> abilityList = character.getAbilities(catagory);
                AbilityFacade[] abilities = abilityList.toArray(new AbilityFacade[0]);
                for (AbilityFacade ability : abilities)
                {
                    abilityMap.put(ability, catagory);
                }
                abilityList.addGenericListDataListener(
                        new AbilityModelListener()
                        {

                            public void intervalAdded(GenericListDataEvent e)
                            {
                                List<AbilityFacade> sublist =
                                        abilityList.subList(e.getIndex0(),
                                                            e.getIndex1() + 1);
                                for (AbilityFacade ability : sublist)
                                {
                                    abilityMap.put(ability, catagory);
                                }
                                model.addAll(sublist);
                            }

                        });
            }
            model.addAll(abilityMap.keySet());
        }

        public List<? extends TreeView<AbilityFacade>> getTreeViews()
        {
            return treeViews;
        }

        public int getDefaultTreeViewIndex()
        {
            return 0;
        }

        public DataView<AbilityFacade> getDataView()
        {
            return abilityDataView;
        }

        public GenericListModel<AbilityFacade> getDataModel()
        {
            return model;
        }

        private abstract class AbilityModelListener implements GenericListDataListener
        {

            public void intervalRemoved(GenericListDataEvent e)
            {
                model.removeAll(e.getData());
                abilityMap.keySet().removeAll(e.getData());
            }

            public void contentsChanged(GenericListDataEvent e)
            {
                intervalRemoved(e);
                intervalAdded(e);
            }

        }

        public Class<AbilityFacade> getFilterClass()
        {
            return AbilityFacade.class;
        }

    }

    private static final class AvailableAbilityTreeViewModel implements FilterableTreeViewModel<AbilityFacade>,
                                                                           GenericListDataListener
    {

        private final GenericListModel<AbilityFacade> dataModel;
        private final CharacterFacade character;
        private AbilityCatagoryFacade catagory;

        public AvailableAbilityTreeViewModel(CharacterFacade character)
        {
            this.dataModel = new GenericListModel<AbilityFacade>();
            this.character = character;
        }

        public List<? extends TreeView<AbilityFacade>> getTreeViews()
        {
            return null;
        }

        public int getDefaultTreeViewIndex()
        {
            return 0;
        }

        public DataView<AbilityFacade> getDataView()
        {
            return abilityDataView;
        }

        public GenericListModel<AbilityFacade> getDataModel()
        {
            return dataModel;
        }

        public void setAbilityCatagory(AbilityCatagoryFacade catagory)
        {
            if (this.catagory != null)
            {
                PCGenUIManager.getRegisteredAbilities(character,
                                                      this.catagory).removeGenericListDataListener(this);
            }
            this.catagory = catagory;

            GenericListModel<AbilityFacade> abilities = PCGenUIManager.getRegisteredAbilities(character,
                                                                                              catagory);
            dataModel.clear();
            addData(abilities);
            abilities.addGenericListDataListener(this);
        }

        private void addData(Collection<AbilityFacade> abilities)
        {
            HashSet<AbilityFacade> abilitySet = new HashSet<AbilityFacade>(abilities);
            Iterator<AbilityFacade> it = abilitySet.iterator();
            while (it.hasNext())
            {
                AbilityFacade ability = it.next();
                if (!ability.isMult() && character.hasAbility(catagory, ability))
                {
                    it.remove();
                }
            }
            dataModel.addAll(abilitySet);
        }

        public void intervalAdded(GenericListDataEvent e)
        {
            @SuppressWarnings("unchecked")
            GenericListModel<AbilityFacade> source = (GenericListModel<AbilityFacade>) e.getSource();
            addData(source.subList(e.getIndex0(), e.getIndex1() + 1));
        }

        public void intervalRemoved(GenericListDataEvent e)
        {
            dataModel.removeAll(e.getData());
        }

        public void contentsChanged(GenericListDataEvent e)
        {
            intervalRemoved(e);
            intervalAdded(e);
        }

        public Class<AbilityFacade> getFilterClass()
        {
            return AbilityFacade.class;
        }

    }

    private static final class CatagoryTableModel extends AbstractTableModel
            implements ListDataListener
    {

        private CharacterFacade character;
        private GenericListModel<AbilityCatagoryFacade> catagories;

        public CatagoryTableModel(CharacterFacade character,
                                   GenericListModel<AbilityCatagoryFacade> catagories)
        {
            this.character = character;
            this.catagories = catagories;
            catagories.addListDataListener(this);
        }

        public int getRowCount()
        {
            return catagories.getSize();
        }

        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if (columnIndex == 1)
            {
                return true;
            }
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            AbilityCatagoryFacade catagory = catagories.getElementAt(rowIndex);
            switch (columnIndex)
            {
                case 0:
                    return catagory;
                case 1:
                    return character.getRemainingSelections(catagory);
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            character.setRemainingSelection(catagories.getElementAt(rowIndex),
                                            (Integer) aValue);
        }

        @Override
        public String getColumnName(int column)
        {
            if (column == 0)
            {
                return "Catagory";
            }
            return "Remaining";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 1)
            {
                return Integer.class;
            }
            return Object.class;
        }

        public void intervalAdded(ListDataEvent e)
        {
            fireTableRowsInserted(e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e)
        {
            fireTableRowsDeleted(e.getIndex0(), e.getIndex1());
        }

        public void contentsChanged(ListDataEvent e)
        {
            fireTableRowsUpdated(e.getIndex0(), e.getIndex1());
        }

    }

    private final class AbilityTransferHandler extends TransferHandler
    {

        private CharacterFacade character;

        public AbilityTransferHandler(CharacterFacade character)
        {
            this.character = character;
        }

        private final DataFlavor abilityFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                                                                  ";class=" +
                                                                  AbilityFacade.class.getName(),
                                                                  null);

        @Override
        public int getSourceActions(JComponent c)
        {
            if (selectedTreeViewPanel.isAncestorOf(c))
            {
                List<Object> data = selectedTreeViewPanel.getSelectedData();
                if (!data.isEmpty() && data.get(0).getClass() ==
                        AbilityFacade.class)
                {
                    return MOVE;
                }
            }
            else
            {
                List<Object> data = availableTreeViewPanel.getSelectedData();
                if (!data.isEmpty() && data.get(0).getClass() ==
                        AbilityFacade.class)
                {
                    AbilityFacade ability = (AbilityFacade) data.get(0);
                    if (ability.isMult())
                    {
                        return COPY;
                    }
                    if (!character.hasAbility(getSelectedCatagory(), ability))
                    {
                        return MOVE;
                    }
                }
            }
            return NONE;
        }

        @Override
        protected Transferable createTransferable(JComponent c)
        {
            JTreeViewPane pane = (JTreeViewPane) c.getParent();
            final AbilityFacade selectedAbility = (AbilityFacade) pane.getSelectedData().get(0);
            return new Transferable()
            {

                public DataFlavor[] getTransferDataFlavors()
                {
                    return new DataFlavor[]{abilityFlavor};
                }

                public boolean isDataFlavorSupported(DataFlavor flavor)
                {
                    return abilityFlavor == flavor;
                }

                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
                {
                    if (!isDataFlavorSupported(flavor))
                    {
                        throw new UnsupportedFlavorException(flavor);
                    }
                    return selectedAbility;
                }

            };
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
        {
            return transferFlavors[0] == abilityFlavor;
        }

        @Override
        public boolean importData(JComponent comp, Transferable t)
        {
            if (selectedTreeViewPanel.isAncestorOf(comp))
            {
                try
                {
                    AbilityFacade ability = (AbilityFacade) t.getTransferData(abilityFlavor);
                    // TODO: add some extra logic
                    character.addAbility(getSelectedCatagory(), ability);
                    return true;
                }
                catch (UnsupportedFlavorException ex)
                {
                    Logger.getLogger(AbilityChooserTab.class.getName()).log(Level.SEVERE,
                                                                            null,
                                                                            ex);
                }
                catch (IOException ex)
                {
                    Logger.getLogger(AbilityChooserTab.class.getName()).log(Level.SEVERE,
                                                                            null,
                                                                            ex);
                }
                return false;
            }
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data,
                                   int action)
        {
            if (action == COPY)
            {
                return;
            }
        }

    }

    public void setSelectedCatagory(AbilityCatagoryFacade catagory)
    {

    }

    public AbilityCatagoryFacade getSelectedCatagory()
    {
        return null;
    }

    public Hashtable<Object, Object> createState(CharacterFacade character,
                                                  GenericListModel<AbilityCatagoryFacade> catagories)
    {
        Hashtable<Object, Object> state = new Hashtable<Object, Object>();
        CatagoryTableModel catagoryModel = new CatagoryTableModel(character,
                                                                  catagories);
        SelectedAbilityTreeViewModel selectedModel = new SelectedAbilityTreeViewModel(character);
        AvailableAbilityTreeViewModel availableModel = new AvailableAbilityTreeViewModel(character);

        state.put("CatagoryTableModel", catagoryModel);
        state.put("SelectedAbilityTreeViewModel", selectedModel);
        state.put("AvailableAbilityTreeViewModel", availableModel);
        return state;
    }

    public void storeState(Hashtable<Object, Object> state)
    {
    //state.put("SelectedCatagory", catagoryTreeTable.getSelectedRow());
    }

    public void restoreState(Hashtable<?, ?> state)
    {
        catagoryTable.setModel((CatagoryTableModel) state.get("CatagoryTableModel"));
        int selectedCatatoryIndex = (Integer) state.get("SelectedCatagory");
        catagoryTable.getSelectionModel().setSelectionInterval(selectedCatatoryIndex,
                                                               selectedCatatoryIndex);
        selectedTreeViewPanel.setTreeViewModel((SelectedAbilityTreeViewModel) state.get("SelectedAbilityTreeViewModel"));
        availableTreeViewPanel.setTreeViewModel((AvailableAbilityTreeViewModel) state.get("AvailableAbilityTreeViewModel"));

    }

    private class PreReqTreeView implements TreeView<AbilityFacade>
    {

        private GenericListModel<AbilityFacade> abilities;

        public PreReqTreeView(GenericListModel<AbilityFacade> abilities)
        {
            this.abilities = abilities;
        }

        public String getViewName()
        {
            return "Prereq Tree";
        }

        public List<TreeViewPath<AbilityFacade>> getPaths(AbilityFacade pobj)
        {
            List<List<AbilityFacade>> abilityPaths = new ArrayList<List<AbilityFacade>>();
            addPaths(abilityPaths, pobj.getRequiredAbilities(),
                     new ArrayList<AbilityFacade>());
            if (abilityPaths.isEmpty())
            {
                return Collections.singletonList(new TreeViewPath<AbilityFacade>(pobj));
            }

            List<TreeViewPath<AbilityFacade>> paths = new ArrayList<TreeViewPath<AbilityFacade>>();
            for (List<AbilityFacade> path : abilityPaths)
            {
                Collections.reverse(path);
                Object[] array = path.toArray();
                for (int x = 0; x < array.length; x++)
                {
                    if (!abilities.contains(array[x]))
                    {
                        array[x] = array[x].toString();
                    }
                }
                paths.add(new TreeViewPath<AbilityFacade>(array, pobj));
            }
            return paths;
        }

        private void addPaths(List<List<AbilityFacade>> abilityPaths,
                               List<AbilityFacade> preAbilities,
                               ArrayList<AbilityFacade> path)
        {
            for (AbilityFacade preAbility : preAbilities)
            {
                @SuppressWarnings("unchecked")
                ArrayList<AbilityFacade> pathclone = (ArrayList<AbilityFacade>) path.clone();
                pathclone.add(preAbility);
                List<AbilityFacade> preAbilities2 = preAbility.getRequiredAbilities();
                if (preAbilities2.isEmpty())
                {
                    abilityPaths.add(pathclone);
                }
                else
                {
                    addPaths(abilityPaths, preAbilities2, pathclone);
                }
            }
        }

    }
}
