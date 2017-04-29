package gui.controllers.implementations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import application.Client;
import entities.SampServer;
import entities.SampServerSerializeable;
import javafx.scene.control.Label;
import logging.Logging;

public class ServerListAllController extends ServerListControllerMain
{
	private static Object deserialzieAndDecompress(final byte[] data)
	{
		try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
				final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
				final ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);)
		{
			final Object object = objectInputStream.readObject();
			return object;
		}
		catch (final IOException | ClassNotFoundException e)
		{
			Logging.logger.log(Level.SEVERE, "Error deserializing and decompressing data", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize()
	{
		super.initialize();

		if (Objects.nonNull(Client.remoteDataService))
		{
			try
			{
				final byte[] serializedData = Client.remoteDataService.getAllServers();
				final List<SampServerSerializeable> serializedServers = (List<SampServerSerializeable>) deserialzieAndDecompress(serializedData);

				servers.addAll(serializedServers.stream()
						.map(server -> new SampServer(server))
						.collect(Collectors.toSet()));
			}
			catch (final RemoteException e)
			{
				Logging.logger.log(Level.SEVERE, "Couldn't retrieve data from server.", e);
				serverTable.setPlaceholder(new Label("Server connection couldn't be established."));
			}
		}
		else
		{
			serverTable.setPlaceholder(new Label("Server connection couldn't be established."));
		}

		updateGlobalInfo();
	}

	@Override
	protected void displayMenu(final List<SampServer> selectedServers, final double posX, final double posY)
	{
		super.displayMenu(selectedServers, posX, posY);

		addToFavouritesMenuItem.setVisible(true);
		removeFromFavouritesMenuItem.setVisible(false);
	}
}