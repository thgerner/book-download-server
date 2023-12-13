## A Spring web server for e-books in ePub format

The book-download-server is Java Spring application for downloading e-books in ePub format. 
The e-books are read from a filesystem while the folders in the web page are shown as
book shelfs and the books are shown with title and the cover as preview.

The book-download-server is intented to run in a docker container. 

The build requieres Maven:
```
mvn install
```
The docker image can also created using maven, however you need docker installed:
```
mvn docker:build
```

## Starting the docker container

The docker container looks for the e-books in a folder ```/opt/bookserver/ebooks``` and creates the preview pictuers in a folder ```/var/books/preview```. The preview pictures of the covers
may be persisted by exporting the cache folder to the host.


### Run as stand alone server

```
docker run --restart unless-stopped -d -p 8080:8080 -v /var/cache/ebooks/:/var/books/preview/ -v /home/thomas/Downloads/eBooks/:/opt/bookserver/ebooks/ book-download-server:0.5
```

### Run as server behind haproxy with own network 'web'

```
docker run --restart unless-stopped -d --network web --name books_service -v /var/cache/ebooks/:/var/books/preview/ -v /var/local/eBooks/:/opt/bookserver/ebooks/ book-download-server:0.5
```
