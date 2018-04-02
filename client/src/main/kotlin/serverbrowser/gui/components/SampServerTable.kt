package serverbrowser.gui.components

import javafx.beans.property.ObjectProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.*
import javafx.scene.text.Text
import serverbrowser.Client
import serverbrowser.data.FavouritesController
import serverbrowser.data.entites.SampServer
import serverbrowser.data.properties.ClientPropertiesController
import serverbrowser.data.properties.ConnectOnDoubleClickProperty
import serverbrowser.util.samp.GTAController
import serverbrowser.util.windows.OSUtility

import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * [TableView] that was made for the ServerList View, contains a special TableRowFactory and
 * allows filtering and sorting.
 *
 * @author Marcel
 * @since 23.09.2017
 */
class SampServerTable : TableView<SampServer>() {
    /**
     * @return [.tableMode]
     */
    var tableMode = SampServerTableMode.FAVOURITES
        private set

    private val connectMenuItem = MenuItem(Client.getString("connectToServer"))
    private val connectWithPasswordMenuItem = MenuItem(Client.getString("connectToServerUsingPassword"))
    private val connectSeparator = SeparatorMenuItem()
    private val addToFavouritesMenuItem = MenuItem(Client.getString("addToFavourites"))
    private val removeFromFavouritesMenuItem = MenuItem(Client.getString("removeFromFavourites"))
    private val copyIpAddressAndPortMenuItem = MenuItem(Client.getString("copyIpAddressAndPort"))
    private val visitWebsiteMenuItem = MenuItem(Client.getString("visitWebsite"))

    private val tableContextMenu = ContextMenu(connectMenuItem, connectWithPasswordMenuItem, connectSeparator, addToFavouritesMenuItem, removeFromFavouritesMenuItem, copyIpAddressAndPortMenuItem, visitWebsiteMenuItem)

    /**
     * @return the [ObservableList] list that contains all data and is mutable
     */
    private val filteredServers = FilteredList(items)
    private val sortedServers = SortedList(filteredServers)

    private val firstIfAnythingSelected: Optional<SampServer>
        get() {
            val selectedServers = selectionModel.selectedItems

            return if (selectedServers.isEmpty()) {
                Optional.empty()
            } else Optional.ofNullable(selectedServers[0])

        }

    /**
     * Constructor; sets the TableRowFactory, the ContextMenu Actions and table settings.
     */
    init {
        items = sortedServers

        selectionModel.selectionMode = SelectionMode.MULTIPLE
        setKeyActions()
        initTableRowFactory()
        setMenuItemDefaultActions()
    }

    private fun setKeyActions() {
        setOnKeyReleased { released ->
            if (tableMode == SampServerTableMode.FAVOURITES && released.code == KeyCode.DELETE) {
                deleteSelectedFavourites()
            }
        }

    }

    private fun setMenuItemDefaultActions() {
        connectMenuItem.setOnAction { _ -> firstIfAnythingSelected.ifPresent { server -> GTAController.tryToConnect(server.address, server.port, "") } }

        connectWithPasswordMenuItem.setOnAction { _ -> GTAController.promptUserForServerPassword().ifPresent { serverPassword -> firstIfAnythingSelected.ifPresent { server -> GTAController.tryToConnect(server.address, server.port, serverPassword) } } }

        visitWebsiteMenuItem.setOnAction { _ -> firstIfAnythingSelected.ifPresent { server -> OSUtility.browse(server.website!!) } }

        addToFavouritesMenuItem.setOnAction { _ ->
            val serverList = selectionModel.selectedItems
            serverList.forEach { FavouritesController.addServerToFavourites(it) }
        }

        removeFromFavouritesMenuItem.setOnAction { _ -> deleteSelectedFavourites() }

        copyIpAddressAndPortMenuItem.setOnAction { _ ->
            val serverOptional = firstIfAnythingSelected

            serverOptional.ifPresent { server ->
                val content = ClipboardContent()
                content.putString(server.address + ":" + server.port)
                Clipboard.getSystemClipboard().setContent(content)
            }
        }
    }

    private fun deleteSelectedFavourites() {
        val alert = Alert(AlertType.CONFIRMATION, Client.getString("sureYouWantToDeleteFavourites"), ButtonType.YES, ButtonType.NO)
        Client.insertAlertOwner(alert)
        alert.title = Client.getString("deleteFavourites")
        val result = alert.showAndWait()

        result.ifPresent { buttonType ->
            if (buttonType == ButtonType.YES) {
                val serverList = selectionModel.selectedItems
                serverList.forEach { FavouritesController.removeServerFromFavourites(it) }
                items.removeAll(serverList)
            }
        }
    }

    private fun initTableRowFactory() {

        setRowFactory { _ ->
            val row = TableRow<SampServer>()

            row.setOnDragOver { event -> event.acceptTransferModes(TransferMode.MOVE) }
            row.setOnDragEntered { _ -> row.styleClass.add("overline") }
            row.setOnDragExited { _ -> row.styleClass.remove("overline") }
            row.setOnDragDetected { event -> onRowDragDetected(row, event) }
            row.setOnDragDropped { event -> onRowDragDropped(row, event) }
            row.setOnMouseClicked { clicked ->
                // A row has been clicked, so we want to hide the previous context menu
                tableContextMenu.hide()

                if (Objects.nonNull(row.item)) {
                    // If there is an item in this row, we want to proceed further
                    handleClick(row, clicked)
                } else {
                    // Otherwise we clear the selection.
                    selectionModel.clearSelection()
                }
            }

            row
        }
    }

    private fun onRowDragDropped(row: TableRow<SampServer>, event: DragEvent) {
        val dragBoard = event.dragboard
        val oldIndexes = dragBoard.getContent(OLD_INDEXES_LIST_DATA_FORMAT) as List<Int>
        val newIndex = items.indexOf(row.item)
        val newIndexCorrect = if (newIndex == -1) items.size else newIndex

        if (oldIndexes.contains(newIndexCorrect)) {
            return
        }

        val firstOfOldIndexes = oldIndexes[0]
        if (newIndexCorrect != firstOfOldIndexes) {
            val draggedServer = ArrayList(selectionModel.selectedItems)
            val reverseAndAdd = {
                draggedServer.reverse()
                draggedServer.forEach { server -> items.add(newIndexCorrect, server) }
            }
            val sortReverseAndRemove = {
                Collections.sort(oldIndexes)
                Collections.reverse(oldIndexes)
                oldIndexes.forEach { index -> items.removeAt(index) }
            }

            if (oldIndexes[0] < newIndexCorrect) {
                reverseAndAdd()
                sortReverseAndRemove()
            } else {
                sortReverseAndRemove()
                reverseAndAdd()
            }
        }
    }

    private fun onRowDragDetected(row: TableRow<SampServer>, event: MouseEvent) {
        val selectedServers = selectionModel.selectedItems
        val rowServer = row.item
        if (items.size <= 1 || selectedServers.isEmpty() || !selectedServers.contains(rowServer)) {
            return
        }

        val clipboardContent = ClipboardContent()

        val selectedServerIndices = selectionModel.selectedItems.stream()
                .map<Int>({ items.indexOf(it) })
                .collect(Collectors.toList())
        clipboardContent[OLD_INDEXES_LIST_DATA_FORMAT] = selectedServerIndices

        val ghostString = StringJoiner(System.lineSeparator())
        selectedServers.forEach { server -> ghostString.add(server.hostname + " - " + server.toString()) }

        val dragBoard = startDragAndDrop(TransferMode.MOVE)
        dragBoard.setDragView(Text(ghostString.toString()).snapshot(null, null), event.x, event.y)
        dragBoard.setContent(clipboardContent)
    }

    private fun handleClick(row: TableRow<SampServer>, clicked: MouseEvent) {
        if (clicked.button == MouseButton.PRIMARY) {
            handleLeftClick(row)
        } else if (clicked.button == MouseButton.SECONDARY) {
            handleRightClick(row, clicked)
        }
    }

    private fun handleRightClick(row: TableRow<SampServer>, clicked: MouseEvent) {
        val selectedServers = selectionModel.selectedItems

        if (selectionModel.selectedIndices.contains(row.index)) {
            // In case the current selection model contains the clicked row, we want to open the
            // context menu on the current selection mode
            displayMenu(selectedServers, clicked.screenX, clicked.screenY)
        } else {
            // Otherwise we will select the clicked item and open the context menu on it
            displayMenu(listOf<SampServer>(row.item), clicked.screenX, clicked.screenY)
        }
    }

    private fun handleLeftClick(row: TableRow<SampServer>) {
        val lastLeftClickTime = row.userData as Long
        val wasDoubleClick = Objects.nonNull(lastLeftClickTime) && System.currentTimeMillis() - lastLeftClickTime < 300
        val onlyOneSelectedItem = selectionModel.selectedItems.size == 1

        if (wasDoubleClick && onlyOneSelectedItem) {
            if (ClientPropertiesController.getProperty(ConnectOnDoubleClickProperty)) {
                firstIfAnythingSelected.ifPresent { server -> GTAController.tryToConnect(server.address, server.port, "") }
            }
        } else {
            row.setUserData(java.lang.Long.valueOf(System.currentTimeMillis()))
        }
    }

    /**
     * Displays the context menu for server entries.
     *
     * @param serverList The list of servers that the context menu actions will affect
     * @param posX X coordinate
     * @param posY Y coordinate
     */
    private fun displayMenu(serverList: List<SampServer>, posX: Double, posY: Double) {
        val sizeEqualsOne = serverList.size == 1

        connectMenuItem.isVisible = sizeEqualsOne
        tableContextMenu.items[1].isVisible = sizeEqualsOne // Separator
        copyIpAddressAndPortMenuItem.isVisible = sizeEqualsOne
        visitWebsiteMenuItem.isVisible = sizeEqualsOne
        connectSeparator.isVisible = sizeEqualsOne

        val favouriteMode = tableMode == SampServerTableMode.FAVOURITES

        addToFavouritesMenuItem.isVisible = !favouriteMode
        removeFromFavouritesMenuItem.isVisible = favouriteMode

        tableContextMenu.show(this, posX, posY)
    }

    /**
     * Sets the mode, which decides how the table will behave.
     *
     * @param mode the mode that the [SampServerTable] will be used for.
     */
    fun setServerTableMode(mode: SampServerTableMode) {
        tableMode = mode
    }

    /**
     * @return the comparator property that is used to sort the items
     */
    fun sortedListComparatorProperty(): ObjectProperty<Comparator<in SampServer>> {
        return sortedServers.comparatorProperty()
    }

    /**
     * @return the predicate property that is used to filter the data
     */
    fun predicateProperty(): ObjectProperty<Predicate<in SampServer>> {
        return filteredServers.predicateProperty()
    }

    /**
     * Returns true if this list contains the specified element
     *
     * @param server the server to search for
     * @return true if the data contains the server
     */
    operator fun contains(server: SampServer): Boolean {
        return items.contains(server)
    }

    /**
     * Deletes all currently contained servers.
     */
    fun clear() {
        items.clear()
    }

    /**
     * Adds a new [SampServer] to the data.
     *
     * @param newServer the server that will be added
     */
    fun add(newServer: SampServer) {
        items.add(newServer)
    }

    /**
     * Adds a collection of new [SampServer] to the data.
     *
     * @param newServers the servers that will be added
     */
    fun addAll(newServers: Collection<SampServer>) {
        newServers.forEach { this.add(it) }
    }

    companion object {
        private val OLD_INDEXES_LIST_DATA_FORMAT = DataFormat("index")
    }
}